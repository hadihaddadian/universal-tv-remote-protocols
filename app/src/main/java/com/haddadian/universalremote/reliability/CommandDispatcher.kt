package com.haddadian.universalremote.reliability

import com.haddadian.universalremote.Command
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes delivery and applies command policy without silently dropping user input.
 * Idempotent commands may be retried once after a failed transport attempt because
 * repeating them is defined to be safe by CommandPolicyEngine.
 */
class RemoteCommandDispatcher {
    private val mutex = Mutex()
    private var lastSentAtMs = 0L

    suspend fun dispatch(
        command: Command,
        policy: RemoteCommandPolicy,
        send: suspend (Command) -> Result<Unit>
    ): Result<Unit> = mutex.withLock {
        val minIntervalMs = when (policy.delivery) {
            CommandDeliveryClass.COALESCABLE -> maxOf(policy.minIntervalMs, 25L)
            CommandDeliveryClass.ORDERED,
            CommandDeliveryClass.IDEMPOTENT -> policy.minIntervalMs
        }
        if (minIntervalMs > 0) {
            val now = System.currentTimeMillis()
            val wait = minIntervalMs - (now - lastSentAtMs)
            if (wait > 0) delay(wait)
        }

        val firstAttempt = send(command)
        val result = if (policy.delivery == CommandDeliveryClass.IDEMPOTENT && firstAttempt.isFailure) {
            // Idempotent commands are explicitly safe to repeat after a transport failure.
            delay(50)
            send(command)
        } else {
            firstAttempt
        }

        if (result.isSuccess) {
            lastSentAtMs = System.currentTimeMillis()
        }
        result
    }
}
