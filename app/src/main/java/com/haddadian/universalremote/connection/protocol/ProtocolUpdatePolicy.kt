package com.haddadian.universalremote.connection.protocol

data class ProtocolReleaseMetadata(
    val protocolId: ProtocolId,
    val version: Int,
    val status: ReleaseStatus,
    val releasedAtEpochMs: Long,
    val minimumAppVersion: Int
)

enum class ReleaseStatus { STABLE, BETA, DEPRECATED, BLOCKED }

class ProtocolUpdatePolicy(private val now: () -> Long = { System.currentTimeMillis() }) {
    fun shouldRefresh(lastCheck: Long?, intervalMs: Long = 24L * 60L * 60L * 1000L): Boolean =
        lastCheck == null || now() - lastCheck >= intervalMs
    fun accepts(metadata: ProtocolReleaseMetadata, appVersion: Int): Boolean =
        metadata.status != ReleaseStatus.BLOCKED && metadata.minimumAppVersion <= appVersion
}
