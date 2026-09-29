package com.haddadian.universalremote.connection

sealed interface PairingState {
    data object NotRequired : PairingState
    data class Waiting(val message: String) : PairingState
    data class Authenticated(val method: String) : PairingState
    data class Failed(val message: String) : PairingState
}

class PairingAssistant {
    fun start(requiresPairing: Boolean, authMode: String): PairingState =
        if (!requiresPairing || authMode.equals("none", true)) PairingState.NotRequired
        else PairingState.Waiting("Pairing is required for $authMode")
}
