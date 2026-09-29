package com.haddadian.universalremote.reliability

import kotlinx.coroutines.delay

/** Bounded recovery with an explicit attempt callback for UI/diagnostics. */
class ConnectionRecoveryCoordinator(private val retryPolicy: RetryPolicy = RetryPolicy()) {
    suspend fun <T> recover(
        maxAttempts: Int = 3,
        networkChanged: () -> Boolean,
        reconnect: suspend () -> Result<T>,
        rediscoverAndReconnect: suspend () -> Result<T>,
        onAttempt: (attempt: Int, rediscovery: Boolean) -> Unit = { _, _ -> }
    ): Result<T> {
        var last: Throwable = IllegalStateException("Recovery did not run")
        repeat(maxAttempts.coerceAtLeast(1)) { index ->
            val attempt = index + 1
            val rediscover = networkChanged()
            onAttempt(attempt, rediscover)
            val result = if (rediscover) rediscoverAndReconnect() else reconnect()
            result.getOrNull()?.let { return Result.success(it) }
            last = result.exceptionOrNull() ?: last
            if (attempt < maxAttempts) delay(retryPolicy.delayForAttempt(attempt))
        }
        return Result.failure(last)
    }
}
