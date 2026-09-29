package com.haddadian.universalremote.connection

interface ConnectionAdapter {
    val transport: ConnectionTransport
    suspend fun probe(device: TvFingerprint): Result<String>
    suspend fun pair(device: TvFingerprint): Result<String>
    suspend fun connect(device: TvFingerprint): Result<String>
    suspend fun disconnect()
}

class AndroidTvRemoteV2Adapter : ConnectionAdapter {
    override val transport = ConnectionTransport.ANDROID_TV_REMOTE_V2
    override suspend fun probe(device: TvFingerprint): Result<String> =
        if (device.platform?.contains("android", true) == true ||
            device.capabilities.contains("android_tv_remote_v2")) {
            Result.success("Android TV Remote v2 service candidate")
        } else {
            Result.failure(IllegalStateException("Android TV Remote v2 not identified"))
        }

    override suspend fun pair(device: TvFingerprint): Result<String> =
        Result.failure(IllegalStateException("Use AndroidTvRemoteV2 pairing flow with the TV PIN"))

    override suspend fun connect(device: TvFingerprint): Result<String> =
        Result.failure(IllegalStateException("Use AndroidTvRemoteV2 connectPaired flow"))

    override suspend fun disconnect() = Unit
}

class MdnsAdapter : ConnectionAdapter {
    override val transport = ConnectionTransport.MDNS
    override suspend fun probe(device: TvFingerprint) = Result.success("mDNS discovery/probe")
    override suspend fun pair(device: TvFingerprint) =
        Result.failure<String>(IllegalStateException("mDNS is discovery, not pairing"))
    override suspend fun connect(device: TvFingerprint) =
        Result.failure<String>(IllegalStateException("Select a concrete TV protocol after discovery"))
    override suspend fun disconnect() = Unit
}

class IrAdapter : ConnectionAdapter {
    override val transport = ConnectionTransport.IR
    override suspend fun probe(device: TvFingerprint) =
        if (device.capabilities.contains("ir_blaster_available")) {
            Result.success("IR blaster available")
        } else {
            Result.failure(IllegalStateException("No IR blaster available"))
        }

    override suspend fun pair(device: TvFingerprint) = Result.success("No pairing required")
    override suspend fun connect(device: TvFingerprint) = Result.success("IR ready")
    override suspend fun disconnect() = Unit
}
