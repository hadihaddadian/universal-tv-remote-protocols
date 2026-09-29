package com.haddadian.universalremote.connection.protocol

import android.content.Context
import com.haddadian.universalremote.Command
import com.haddadian.universalremote.Credentials
import com.haddadian.universalremote.TvDevice
import com.haddadian.universalremote.TvTransport
import com.haddadian.universalremote.connection.ConnectionTransport
import com.haddadian.universalremote.connection.TvFingerprint
import com.haddadian.universalremote.domain.Capability
import com.haddadian.universalremote.domain.EffectiveCapabilityResolver
import com.haddadian.universalremote.reliability.ConnectionHealth

/** Single executable bridge from protocol resolution to compiled transports. */
class ProtocolTransportExecutor(
    private val manager: ProtocolManager,
    private val bindings: ProtocolBindingRegistry,
    private val context: Context
) {
    private var active: ActiveProtocol? = null

    suspend fun prepare(device: TvDevice): Result<ProtocolProfile> {
        if (active?.deviceId != device.id) disconnectCurrent()
        val result = manager.prepareFor(device.toFingerprint())
        val profile = when (result) {
            is ProtocolManagerResult.Success -> result.value
            is ProtocolManagerResult.Failure -> return Result.failure(result.error)
        }
        if (profile.transports.isEmpty()) return Result.failure(IllegalStateException("Protocol has no transport: ${profile.manifest.id}"))
        val binding = profile.transports.firstOrNull { bindings.supports(profile.manifest.id, it) }
            ?: return Result.failure(IllegalStateException("Protocol ${profile.manifest.id} is declarative-only or has no executable binding"))

        val transport = bindings.createTransport(profile.manifest.id, setOf(binding), context)
            ?: return Result.failure(IllegalStateException("No executable adapter for ${profile.manifest.id} / $binding"))
        active = ActiveProtocol(device, device.id, profile.manifest.id, transport, profile)
        return Result.success(profile)
    }

    suspend fun connect(device: TvDevice, credentials: Credentials = Credentials()): Result<Unit> {
        if (active?.deviceId != device.id) prepare(device).getOrElse { return Result.failure(it) }
        return active?.transport?.connect(device, credentials) ?: Result.success(Unit)
    }

    suspend fun send(device: TvDevice, command: Command): Result<Unit> {
        val current = active ?: return Result.failure(IllegalStateException("Protocol is not prepared"))
        if (current.deviceId != device.id) return Result.failure(IllegalStateException("Active protocol belongs to a different device"))
        val required = when (command) {
            Command.Power -> Capability.POWER
            Command.VolumeUp, Command.VolumeDown -> Capability.VOLUME
            Command.Mute -> Capability.MUTE
            Command.PlayPause, Command.Stop, Command.Previous, Command.Next, Command.Rewind, Command.FastForward -> Capability.PLAYBACK
            Command.Guide -> Capability.NAVIGATION
            Command.Input -> Capability.INPUT
            is Command.Number -> Capability.NUMBER_PAD
            is Command.SendText -> Capability.KEYBOARD
            is Command.LaunchApp -> Capability.APP_LAUNCH
            Command.Home, Command.Back, Command.Up, Command.Down, Command.Left, Command.Right, Command.Ok -> Capability.NAVIGATION
        }
        if (required !in effectiveCapabilities(device)) {
            return Result.failure(UnsupportedOperationException("Command ${command::class.simpleName} is not supported by the active protocol/device"))
        }
        return when (command) {
            is Command.SendText -> current.transport.sendText(device, command.text)
            is Command.LaunchApp -> {
                val link = command.appLink.trim()
                val scheme = runCatching { java.net.URI(link).scheme?.lowercase() }.getOrNull()
                if (link.length > 512 || scheme !in setOf("https", "http")) {
                    Result.failure(IllegalArgumentException("App Link must be an HTTP(S) URI"))
                } else {
                    current.transport.launchApp(device, link)
                }
            }
            else -> current.transport.send(device, command)
        }
    }

    suspend fun startVoice(device: TvDevice, scope: kotlinx.coroutines.CoroutineScope): Result<Unit> {
        val current = active ?: return Result.failure(IllegalStateException("Protocol is not prepared"))
        if (current.deviceId != device.id) return Result.failure(IllegalStateException("Active protocol belongs to a different device"))
        if (Capability.VOICE !in effectiveCapabilities(device)) return Result.failure(UnsupportedOperationException("Voice is not supported by this device"))
        return current.transport.startVoice(scope)
    }

    suspend fun stopVoice() { active?.transport?.stopVoice() }

    suspend fun healthCheck(device: TvDevice): ConnectionHealth {
        val current = active ?: return ConnectionHealth.UNKNOWN
        if (current.deviceId != device.id) return ConnectionHealth.UNKNOWN
        return current.transport.healthCheck(device)
    }

    fun effectiveCapabilities(device: TvDevice): Set<Capability> {
        val current = active ?: return emptySet()
        if (current.deviceId != device.id) return emptySet()
        return EffectiveCapabilityResolver.resolveCanonical(current.profile.capabilities, device.capabilities)
    }

    suspend fun disconnectCurrent() {
        val current = active ?: return
        runCatching { current.transport.disconnect(current.device) }
        active = null
    }

    fun clear() {
        active?.transport?.close()
        active = null
    }

    private data class ActiveProtocol(
        val device: TvDevice,
        val deviceId: String,
        val protocolId: ProtocolId,
        val transport: TvTransport,
        val profile: ProtocolProfile
    )
}

private fun TvDevice.toFingerprint(): TvFingerprint = TvFingerprint(
    name = name,
    manufacturer = manufacturer ?: when (protocol.lowercase()) {
        "samsung", ProtocolIds.SAMSUNG_WS -> "Samsung"
        "webos", ProtocolIds.LG_SSAP -> "LG"
        "sony", ProtocolIds.SONY_IRCC -> "Sony"
        "philips", ProtocolIds.PHILIPS_JOINTSPACE -> "Philips"
        "roku", ProtocolIds.ROKU_ECP -> "Roku"
        "androidtv", ProtocolIds.ANDROID_TV_REMOTE_V2, "googletv" -> "Google"
        else -> null
    },
    model = model,
    platform = when (protocol.lowercase()) {
        "webos", ProtocolIds.LG_SSAP -> "webos"
        "androidtv", ProtocolIds.ANDROID_TV_REMOTE_V2, "googletv" -> "androidtv"
        "ir", ProtocolIds.GENERIC_IR_TV -> "ir"
        "samsung", ProtocolIds.SAMSUNG_WS -> "samsung"
        "sony", ProtocolIds.SONY_IRCC -> "sony"
        "philips", ProtocolIds.PHILIPS_JOINTSPACE -> "philips"
        "roku", ProtocolIds.ROKU_ECP -> "roku"
        else -> protocol
    },
    address = address,
    capabilities = capabilities
)
