package com.haddadian.universalremote.reliability

import kotlin.math.min

/** Bounded exponential backoff; callers decide whether an error is retryable. */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 500,
    val maxDelayMs: Long = 4_000
) {
    fun delayForAttempt(attempt: Int): Long {
        require(attempt >= 1)
        return min(maxDelayMs, initialDelayMs * (1L shl (attempt - 1).coerceAtMost(20)))
    }
}
