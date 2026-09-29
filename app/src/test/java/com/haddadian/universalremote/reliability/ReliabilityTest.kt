package com.haddadian.universalremote.reliability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReliabilityTest {
    @Test fun retryIsBounded() {
        val p = RetryPolicy(maxAttempts = 3, initialDelayMs = 100, maxDelayMs = 250)
        assertEquals(100, p.delayForAttempt(1))
        assertEquals(200, p.delayForAttempt(2))
        assertEquals(250, p.delayForAttempt(3))
    }
    @Test fun stateMachineRejectsImpossibleJump() {
        val machine = ConnectionStateMachine()
        assertTrue(!machine.transitionTo(ReliableConnectionState.Ready))
        assertTrue(machine.transitionTo(ReliableConnectionState.Discovering))
    }
}
