package com.haddadian.universalremote.ai

import org.junit.Assert.*
import org.junit.Test

class AiBoundaryTest {
    @Test fun rejectsArbitraryExecution() {
        assertTrue(AiCommandBoundary().validate(AiCommand("SHELL", null, 1f)).isFailure)
    }

    @Test fun rejectsBadVolume() {
        assertTrue(AiCommandBoundary().validate(AiCommand("SET_VOLUME", "101", 1f)).isFailure)
    }

    @Test fun acceptsRemoteCommand() {
        assertTrue(AiCommandBoundary().validate(AiCommand("VOLUME_UP", null, .99f)).isSuccess)
    }
}