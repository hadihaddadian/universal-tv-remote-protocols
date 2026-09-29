package com.haddadian.universalremote.adapters

interface ProtocolAdapter {
    val id: String
    val platform: String
    suspend fun discover(): List<DeviceCandidate>
    suspend fun connect(device: DeviceCandidate): Result<Unit>
    fun disconnect()
    fun key(key: RemoteKey): Result<Unit>
    fun text(value: String): Result<Unit>
    fun volume(delta: Int): Result<Unit>
}

data class DeviceCandidate(
    val id: String,
    val host: String,
    val port: Int?,
    val manufacturer: String?,
    val model: String?,
    val platform: String,
    val confidence: Int
)

enum class RemoteKey {
    POWER, HOME, BACK, UP, DOWN, LEFT, RIGHT, OK,
    VOLUME_UP, VOLUME_DOWN, MUTE, PLAY_PAUSE, STOP, PREVIOUS, NEXT, REWIND, FAST_FORWARD, GUIDE, INPUT, SETTINGS
}
