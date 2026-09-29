package com.haddadian.universalremote

import android.content.Context
import com.haddadian.universalremote.reliability.ConnectionHealth

/** Canonical ProtocolManager bridge for Android TV Remote v2. Pairing remains protocol-specific. */
class AndroidTvTransport(context: Context) : TvTransport {
    private val appContext = context.applicationContext
    private var engine: AndroidTvRemoteEngine? = null
    private var voice: VoiceRemoteController? = null

    override suspend fun connect(device: TvDevice, auth: Credentials): Result<Unit> = runCatching {
        val host = requireNotNull(device.address) { "Android TV IP missing" }
        engine?.close()
        val e = AndroidTvRemoteEngine(AtvCertificateStore(appContext))
        runCatching { e.connect(host, device.id).getOrThrow() }
            .getOrElse { e.close(); throw it }
        engine = e
        Unit
    }

    override suspend fun startVoice(scope: kotlinx.coroutines.CoroutineScope): Result<Unit> =
        engine?.let { voice = VoiceRemoteController(appContext, it, scope); voice!!.start() }
            ?: Result.failure(IllegalStateException("Android TV is not connected"))

    override suspend fun stopVoice() { voice?.stop(); voice = null }

    override suspend fun sendText(device: TvDevice, text: String): Result<Unit> =
        engine?.sendText(text) ?: Result.failure(IllegalStateException("Android TV is not connected"))

    override suspend fun launchApp(device: TvDevice, appLink: String): Result<Unit> =
        engine?.launchApp(appLink) ?: Result.failure(IllegalStateException("Android TV is not connected"))

    override suspend fun send(device: TvDevice, command: Command): Result<Unit> {
        val e = engine ?: return Result.failure(IllegalStateException("Android TV is not connected"))
        return when (command) {
            Command.VolumeUp -> e.volumeDelta(1)
            Command.VolumeDown -> e.volumeDelta(-1)
            Command.PlayPause -> e.sendKey(85)
            Command.Stop -> e.sendKey(86)
            Command.Previous -> e.sendKey(88)
            Command.Next -> e.sendKey(87)
            Command.Rewind -> e.sendKey(89)
            Command.FastForward -> e.sendKey(90)
            else -> e.sendKey(when (command) {
                Command.Up -> 19
                Command.Down -> 20
                Command.Left -> 21
                Command.Right -> 22
                Command.Ok -> 23
                Command.Back -> 4
                Command.Home -> 3
                Command.Power -> 26
                Command.Mute -> 164
                Command.Guide -> 172
                Command.Input -> 178
                is Command.Number -> if (command.value in 0..9) 7 + command.value else return Result.failure(IllegalArgumentException("Number must be 0..9"))
                else -> return Result.success(Unit)
            })
        }
    }

    override suspend fun healthCheck(device: TvDevice): ConnectionHealth =
        if (engine?.isConnected() == true) ConnectionHealth.HEALTHY else ConnectionHealth.UNHEALTHY

    override suspend fun disconnect(device: TvDevice) { close() }
    override fun close() {
        voice = null
        engine?.close()
        engine = null
    }
}
