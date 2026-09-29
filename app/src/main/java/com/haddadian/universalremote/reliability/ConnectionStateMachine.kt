package com.haddadian.universalremote.reliability

sealed interface ReliableConnectionState {
    data object Idle : ReliableConnectionState
    data object Discovering : ReliableConnectionState
    data object DeviceFound : ReliableConnectionState
    data object Identifying : ReliableConnectionState
    data object ResolvingProtocol : ReliableConnectionState
    data object PairingRequired : ReliableConnectionState
    data object Pairing : ReliableConnectionState
    data object Connecting : ReliableConnectionState
    data object Authenticating : ReliableConnectionState
    data object Ready : ReliableConnectionState
    data class Reconnecting(val attempt: Int) : ReliableConnectionState
    data class Failed(val reason: String) : ReliableConnectionState
    data object Disconnected : ReliableConnectionState
}

class ConnectionStateMachine(initial: ReliableConnectionState = ReliableConnectionState.Idle) {
    var state: ReliableConnectionState = initial
        private set

    fun transitionTo(next: ReliableConnectionState): Boolean {
        if (!allowed(state, next)) return false
        state = next
        return true
    }

    private fun allowed(from: ReliableConnectionState, to: ReliableConnectionState): Boolean = when (from) {
        ReliableConnectionState.Idle -> to is ReliableConnectionState.Discovering || to is ReliableConnectionState.DeviceFound || to is ReliableConnectionState.Identifying || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.Discovering -> to is ReliableConnectionState.DeviceFound || to is ReliableConnectionState.Idle || to is ReliableConnectionState.Failed || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.DeviceFound -> to is ReliableConnectionState.Identifying || to is ReliableConnectionState.Ready || to is ReliableConnectionState.Idle || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.Identifying -> to is ReliableConnectionState.ResolvingProtocol || to is ReliableConnectionState.Failed || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.ResolvingProtocol -> to is ReliableConnectionState.PairingRequired || to is ReliableConnectionState.Connecting || to is ReliableConnectionState.Failed || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.PairingRequired -> to is ReliableConnectionState.Pairing || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.Pairing -> to is ReliableConnectionState.Authenticating || to is ReliableConnectionState.Failed || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.Authenticating -> to is ReliableConnectionState.Connecting || to is ReliableConnectionState.Failed || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.Connecting -> to is ReliableConnectionState.Ready || to is ReliableConnectionState.Reconnecting || to is ReliableConnectionState.Failed || to is ReliableConnectionState.Disconnected
        ReliableConnectionState.Ready -> to is ReliableConnectionState.Identifying || to is ReliableConnectionState.DeviceFound || to is ReliableConnectionState.Reconnecting || to is ReliableConnectionState.Disconnected || to is ReliableConnectionState.Failed || to is ReliableConnectionState.Discovering
        is ReliableConnectionState.Reconnecting -> to is ReliableConnectionState.Connecting || to is ReliableConnectionState.Failed || to is ReliableConnectionState.Disconnected
        is ReliableConnectionState.Failed -> to is ReliableConnectionState.Reconnecting || to is ReliableConnectionState.Identifying || to is ReliableConnectionState.Disconnected || to is ReliableConnectionState.Discovering || to is ReliableConnectionState.Idle || to is ReliableConnectionState.DeviceFound
        ReliableConnectionState.Disconnected -> to is ReliableConnectionState.Idle || to is ReliableConnectionState.Discovering
    }
}
