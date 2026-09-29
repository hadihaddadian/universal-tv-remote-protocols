package com.haddadian.universalremote.application

import com.haddadian.universalremote.connection.ConnectionAdapter
import com.haddadian.universalremote.connection.ConnectionOrchestrator
import com.haddadian.universalremote.connection.ConnectionTransport
import com.haddadian.universalremote.connection.TvFingerprint
import com.haddadian.universalremote.domain.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application facade. Protocol resolution/execution remains in the connection layer. */
class SmartConnectionEngine(
    adapters: Map<ConnectionTransport, ConnectionAdapter>,
    protocolManager: com.haddadian.universalremote.connection.protocol.ProtocolManager? = null,
    bindingRegistry: com.haddadian.universalremote.connection.protocol.ProtocolBindingRegistry = com.haddadian.universalremote.connection.protocol.ProtocolBindingRegistry.fromBuiltIns()
) {
    private val _state = MutableStateFlow<ConnectionEngineState>(ConnectionEngineState.Idle)
    val state: StateFlow<ConnectionEngineState> = _state.asStateFlow()

    private val orchestrator = ConnectionOrchestrator(
        adapters = adapters,
        onStateChanged = { state -> _state.value = ConnectionEngineState.FromOrchestrator(state) },
        protocolManager = protocolManager,
        bindingRegistry = bindingRegistry
    )

    suspend fun connect(device: TvDevice): Result<TvDevice> {
        val fingerprint = TvFingerprint(
            name = device.name,
            manufacturer = device.manufacturer,
            model = device.model,
            address = device.address,
            platform = device.protocol,
            capabilities = device.capabilities
        )
        return orchestrator.connect(fingerprint).map { transport ->
            device.copy(transport = transport.name)
        }
    }
}

sealed interface ConnectionEngineState {
    data object Idle : ConnectionEngineState
    data class FromOrchestrator(val state: com.haddadian.universalremote.connection.ConnectionState) : ConnectionEngineState
}
