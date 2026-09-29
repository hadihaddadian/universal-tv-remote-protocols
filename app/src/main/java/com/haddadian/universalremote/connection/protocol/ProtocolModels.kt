package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.ConnectionTransport

/** Stable identity for a protocol package. Never use a display name as identity. */
data class ProtocolId(val value: String) {
    init { require(value.matches(Regex("[a-z0-9][a-z0-9._-]{1,63}"))) }
    override fun toString(): String = value
}

enum class ProtocolSource { BUILTIN, INSTALLED, REMOTE }
enum class ProtocolTrust { BUILTIN, SIGNED }
enum class ProtocolPackageState { AVAILABLE, INSTALLED, UPDATE_AVAILABLE, REJECTED }

data class ProtocolPackageManifest(
    val id: ProtocolId,
    val manufacturer: String,
    val platform: String,
    val version: Int,
    val minAppVersion: Int,
    val modelPatterns: Set<String> = emptySet(),
    val payloadSha256: String,
    val signatureBase64: String?,
    val trust: ProtocolTrust,
    val source: ProtocolSource
)

data class ProtocolCommandDefinition(
    val command: String,
    val wireValue: String
)

/** Canonical mapping between declarative protocol commands and capabilities. */
object ProtocolCommandCapabilities {
    val capabilityByCommand: Map<String, String> = linkedMapOf(
        "Power" to "power",
        "Home" to "navigation",
        "Back" to "navigation",
        "Up" to "navigation",
        "Down" to "navigation",
        "Left" to "navigation",
        "Right" to "navigation",
        "Ok" to "navigation",
        "Guide" to "navigation",
        "VolumeUp" to "volume",
        "VolumeDown" to "volume",
        "Mute" to "mute",
        "PlayPause" to "playback",
        "Stop" to "playback",
        "Previous" to "playback",
        "Next" to "playback",
        "Rewind" to "playback",
        "FastForward" to "playback",
        "Input" to "input",
        "Number" to "number_pad",
        "SendText" to "keyboard",
        "LaunchApp" to "app_launch"
    )
}

data class ProtocolProfile(
    val manifest: ProtocolPackageManifest,
    /** Vendors/models may share the same protocol core. This is metadata, not package identity. */
    val supportedManufacturers: Set<String> = emptySet(),
    val discoveryHints: Set<String> = emptySet(),
    val transports: Set<ConnectionTransport> = emptySet(),
    val authMode: String = "none",
    val commands: List<ProtocolCommandDefinition> = emptyList(),
    val capabilities: Set<String> = emptySet()
)

/**
 * v29 fix: a `data class` with a raw ByteArray property gets generated
 * equals()/hashCode() that compare array *reference identity*, not content
 * -- two packages with byte-for-byte identical payloads (e.g. re-downloaded
 * or re-decoded from disk) would compare unequal, and using this class as a
 * map/set key would be unreliable. equals()/hashCode() are now overridden to
 * compare payloadBytes by content, matching what every caller actually
 * expects (identity is `manifest.id + payloadSha256`, not object identity).
 */
data class ProtocolPackage(
    val manifest: ProtocolPackageManifest,
    val profile: ProtocolProfile,
    val payloadBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProtocolPackage) return false
        return manifest == other.manifest &&
            profile == other.profile &&
            payloadBytes.contentEquals(other.payloadBytes)
    }

    override fun hashCode(): Int {
        var result = manifest.hashCode()
        result = 31 * result + profile.hashCode()
        result = 31 * result + payloadBytes.contentHashCode()
        return result
    }
}

data class ProtocolMatch(
    val profile: ProtocolProfile,
    val score: Int,
    val reasons: List<String>
)

enum class ProtocolInstallDecision { USE_INSTALLED, INSTALL, UPDATE }

data class ProtocolManagerState(
    val initialized: Boolean = false,
    val loading: Boolean = false,
    val activeProtocol: ProtocolProfile? = null,
    val installed: List<ProtocolProfile> = emptyList(),
    val error: String? = null
)

sealed interface ProtocolManagerResult<out T> {
    data class Success<T>(val value: T) : ProtocolManagerResult<T>
    data class Failure(val error: ProtocolManagerError) : ProtocolManagerResult<Nothing>
}

sealed class ProtocolManagerError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotFound(id: ProtocolId) : ProtocolManagerError("Protocol not found: $id")
    class InvalidPackage(reason: String) : ProtocolManagerError("Invalid protocol package: $reason")
    class VerificationFailed : ProtocolManagerError("Protocol package verification failed")
    class Incompatible(reason: String) : ProtocolManagerError("Protocol package is incompatible: $reason")
    class Storage(reason: String, cause: Throwable? = null) : ProtocolManagerError(reason, cause)
    class Network(reason: String, cause: Throwable? = null) : ProtocolManagerError(reason, cause)
}
