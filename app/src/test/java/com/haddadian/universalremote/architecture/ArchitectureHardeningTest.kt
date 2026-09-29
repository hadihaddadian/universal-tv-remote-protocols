package com.haddadian.universalremote.architecture

import com.haddadian.universalremote.connection.*
import com.haddadian.universalremote.connection.protocol.*
import com.haddadian.universalremote.domain.CanonicalCommand
import com.haddadian.universalremote.security.CommandSecurityPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchitectureHardeningTest {
    @Test fun sharedAndroidTvProtocolIsRankedForAndroid() {
        val candidates = ConnectionStrategyEngine().rank(
            TvFingerprint(platform = "androidtv"),
            setOf(ConnectionTransport.ANDROID_TV_REMOTE_V2, ConnectionTransport.IR),
            setOf(ConnectionTransport.ANDROID_TV_REMOTE_V2)
        )
        assertEquals(ConnectionTransport.ANDROID_TV_REMOTE_V2, candidates.first().transport)
    }

    @Test fun commandPolicyRejectsInvalidVolume() {
        assertTrue(!CommandSecurityPolicy().isAllowed(CanonicalCommand.SetVolume(101)))
    }

    @Test fun diagnosticRecorderIsBounded() {
        val recorder = DiagnosticRecorder(2)
        recorder.record(ConnectionStage.PROBE, "a")
        recorder.record(ConnectionStage.PROBE, "b")
        recorder.record(ConnectionStage.PROBE, "c")
        assertEquals(2, recorder.snapshot().size)
    }
}
