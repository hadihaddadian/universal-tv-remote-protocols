package com.haddadian.universalremote.domain

import com.haddadian.universalremote.Command

/**
 * Capability-matrix domain model.
 *
 * NOTE (v29 fix): this file used to redeclare `TvDevice`, `ConnectionMethod`,
 * `ConnectionPhase` and `TvFingerprint` with the SAME names as the live UI
 * model in RemoteModels.kt, in the SAME package. Two conflicting
 * declarations of the same name in the same package do not compile in
 * Kotlin ("conflicting overloads"/"redeclaration"). RemoteModels.kt is the
 * definitive UI-facing model (it is what DeviceDiscoveryService, Auth.kt and
 * RemoteViewModel are actually built against), so the types unique to this
 * file have been prefixed with `Profile` to make them distinct while
 * preserving every consumer's behavior (see CapabilityMatrix.kt,
 * AiCommandEngine.kt, application/ConnectionAssistant.kt,
 * application/MacroEngine.kt). The removed ConnectionState/ConnectionPhase
 * duplicates now reuse the ones already defined in RemoteModels.kt.
 */
enum class ProfileTransport { HTTPS, LOCAL_HTTP, TCP, BLUETOOTH, INFRARED }

enum class RemoteMode { DPAD, TOUCHPAD, MOUSE, KEYBOARD }

enum class TvCapability {
    POWER, NAVIGATION, VOLUME, MUTE, CHANNEL, INPUT, NUMBER_PAD,
    TOUCHPAD, POINTER, KEYBOARD, CLIPBOARD, APP_LAUNCH, VOICE_INPUT, PLAYBACK, IR_LEARNING
}

/** Capability-lookup device shape used only by CapabilityMatrix/AiCommandEngine. */
data class ProfileDevice(
    val id: String,
    val manufacturer: String,
    val model: String? = null,
    val firmware: String? = null,
    val address: String? = null,
    val transport: ProfileTransport? = null,
    val requiresPairing: Boolean = false,
    val capabilities: Set<TvCapability> = emptySet(),
    val room: String? = null
)

data class MacroStep(val command: Command, val delayMs: Long = 0L)
data class RemoteMacro(val id: String, val name: String, val steps: List<MacroStep>)
