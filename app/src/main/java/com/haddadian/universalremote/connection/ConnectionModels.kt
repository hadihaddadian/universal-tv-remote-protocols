package com.haddadian.universalremote.connection

enum class ConnectionTransport {
    ANDROID_TV_REMOTE_V2, MDNS, WIFI_DIRECT, BLUETOOTH, IR, ADB, BRAND_PROTOCOL, EXTERNAL_BRIDGE
}

enum class ConnectionStage {
    DISCOVER, FINGERPRINT, PROBE, PAIR, VERIFY, CONNECT, RECOVERED, FAILED
}

data class TvFingerprint(
    val name: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val platform: String? = null,
    val address: String? = null,
    val port: Int? = null,
    val capabilities: Set<String> = emptySet()
)

data class ConnectionCandidate(
    val transport: ConnectionTransport,
    val score: Int,
    val reason: String,
    val requiresPairing: Boolean = false
)

data class ConnectionAttempt(
    val transport: ConnectionTransport,
    val stage: ConnectionStage,
    val success: Boolean,
    val detail: String = ""
)

data class ConnectionState(
    val device: TvFingerprint? = null,
    val stage: ConnectionStage = ConnectionStage.DISCOVER,
    val candidates: List<ConnectionCandidate> = emptyList(),
    val attempts: List<ConnectionAttempt> = emptyList(),
    val selected: ConnectionTransport? = null,
    val connected: Boolean = false,
    val userMessage: String = ""
)
