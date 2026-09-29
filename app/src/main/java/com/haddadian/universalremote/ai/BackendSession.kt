package com.haddadian.universalremote.ai

data class BackendSession(
    val accessToken: String,
    val expiresAtEpochSeconds: Long
) {
    fun isExpired(now: Long) = now >= expiresAtEpochSeconds
    fun shouldRefresh(now: Long, margin: Long = 60) =
        now + margin >= expiresAtEpochSeconds
}

interface BackendSessionProvider {
    suspend fun obtain(nowEpochSeconds: Long): Result<BackendSession>
    suspend fun invalidate()
}