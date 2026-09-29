package com.haddadian.universalremote.connection

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionOrchestratorTest {
    @Test
    fun androidTvIsPreferredAndStopsForPairing() = runBlocking {
        val adapter = object : ConnectionAdapter {
            override val transport = ConnectionTransport.ANDROID_TV_REMOTE_V2
            override suspend fun probe(device: TvFingerprint) = Result.success("ok")
            override suspend fun pair(device: TvFingerprint) = Result.success("paired")
            override suspend fun connect(device: TvFingerprint) = Result.success("connected")
            override suspend fun disconnect() = Unit
        }

        var last: ConnectionState? = null
        val orchestrator = ConnectionOrchestrator(
            adapters = mapOf(ConnectionTransport.ANDROID_TV_REMOTE_V2 to adapter),
            onStateChanged = { last = it }
        )

        val result = orchestrator.connect(
            TvFingerprint(
                platform = "Android TV",
                capabilities = setOf("android_tv_remote_v2")
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PairingRequiredException)
        assertEquals(ConnectionStage.PAIR, last?.stage)
        assertEquals(ConnectionTransport.ANDROID_TV_REMOTE_V2, last?.selected)
        assertTrue(last?.connected == false)
    }
}
