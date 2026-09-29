package com.haddadian.universalremote.reliability

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ConnectionHealth { UNKNOWN, HEALTHY, UNHEALTHY }

/** Periodic health monitor that survives individual probe failures. */
class ConnectionHealthMonitor(
    private val intervalMs: Long = 15_000L
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope, check: suspend () -> ConnectionHealth, onUpdate: (ConnectionHealth) -> Unit) {
        stop()
        job = scope.launch {
            while (isActive) {
                val result = runCatching { check() }.getOrDefault(ConnectionHealth.UNHEALTHY)
                onUpdate(result)
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
