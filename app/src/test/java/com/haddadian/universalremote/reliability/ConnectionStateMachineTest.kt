package com.haddadian.universalremote.reliability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateMachineTest {
    @Test
    fun readyCanSwitchDirectlyToAnotherSelectedDevice() {
        val machine = ConnectionStateMachine(ReliableConnectionState.Ready)
        assertTrue(machine.transitionTo(ReliableConnectionState.DeviceFound))
        assertEquals(ReliableConnectionState.DeviceFound, machine.state)
    }

    @Test
    fun pairingCanBeCancelled() {
        val machine = ConnectionStateMachine(ReliableConnectionState.Pairing)
        assertTrue(machine.transitionTo(ReliableConnectionState.Disconnected))
    }

    @Test
    fun readyCanStartANewConnectionAttempt() {
        val machine = ConnectionStateMachine(ReliableConnectionState.Ready)
        assertTrue(machine.transitionTo(ReliableConnectionState.Identifying))
        assertEquals(ReliableConnectionState.Identifying, machine.state)
    }
}
