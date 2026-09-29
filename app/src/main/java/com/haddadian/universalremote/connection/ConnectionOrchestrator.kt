package com.haddadian.universalremote.connection

import com.haddadian.universalremote.connection.protocol.ProtocolBindingRegistry
import com.haddadian.universalremote.connection.protocol.ProtocolManager
import com.haddadian.universalremote.connection.protocol.ProtocolManagerResult

/**
 * Executes connection attempts. Protocol selection belongs to ProtocolManager;
 * executable transport availability belongs to ProtocolBindingRegistry + adapters.
 */
class ConnectionOrchestrator(
    private val adapters: Map<ConnectionTransport, ConnectionAdapter>,
    private val onStateChanged: (ConnectionState) -> Unit = {},
    private val protocolManager: ProtocolManager? = null,
    private val bindingRegistry: ProtocolBindingRegistry = ProtocolBindingRegistry.fromBuiltIns(),
    private val strategy: ConnectionStrategyEngine = ConnectionStrategyEngine(),
    private val diagnostics: DiagnosticRecorder = DiagnosticRecorder()
) {
    suspend fun connect(device: TvFingerprint): Result<ConnectionTransport> {
        val protocol = protocolManager?.let { manager ->
            when (val result = manager.prepareFor(device)) {
                is ProtocolManagerResult.Success -> result.value
                is ProtocolManagerResult.Failure -> {
                    val state = ConnectionState(
                        device = device,
                        stage = ConnectionStage.FAILED,
                        userMessage = result.error.message ?: "Protocol preparation failed"
                    )
                    onStateChanged(state)
                    return Result.failure(result.error)
                }
            }
        }

        diagnostics.record(ConnectionStage.PROBE, "Protocol resolved: ${protocol?.manifest?.id ?: "none"}")
        val candidates = strategy.rank(device, adapters.keys, protocol?.transports.orEmpty())
            .filter { candidate ->
                protocol == null || bindingRegistry.supports(protocol.manifest.id, candidate.transport)
            }
            .map { candidate ->
                val androidTvRequiresPairing =
                    candidate.transport == ConnectionTransport.ANDROID_TV_REMOTE_V2 &&
                        device.capabilities.any { it.equals("android_tv_remote_v2", true) }
                candidate.copy(
                    requiresPairing = candidate.requiresPairing ||
                        androidTvRequiresPairing ||
                        (protocol?.authMode?.equals("none", true) == false)
                )
            }
        var state = ConnectionState(
            device = device,
            stage = ConnectionStage.PROBE,
            candidates = candidates,
            userMessage = "Testing connection methods"
        )
        onStateChanged(state)

        for (candidate in candidates) {
            val adapter = adapters[candidate.transport] ?: continue
            val attempts = state.attempts.toMutableList()

            diagnostics.record(ConnectionStage.PROBE, "Probing ${candidate.transport}")
            val probe = runCatching { adapter.probe(device) }.getOrElse { Result.failure(it) }
            attempts += ConnectionAttempt(
                candidate.transport,
                ConnectionStage.PROBE,
                probe.isSuccess,
                probe.exceptionOrNull()?.message ?: probe.getOrNull().orEmpty()
            )
            state = state.copy(attempts = attempts)
            onStateChanged(state)
            if (probe.isFailure) continue

            if (candidate.requiresPairing) {
                state = state.copy(stage = ConnectionStage.PAIR, selected = candidate.transport)
                onStateChanged(state)
                return Result.failure(PairingRequiredException(candidate.transport, "User pairing is required"))
            }

            state = state.copy(stage = ConnectionStage.CONNECT, selected = candidate.transport)
            onStateChanged(state)
            val connected = runCatching { adapter.connect(device) }.getOrElse { Result.failure(it) }
            attempts += ConnectionAttempt(
                candidate.transport,
                ConnectionStage.CONNECT,
                connected.isSuccess,
                connected.exceptionOrNull()?.message ?: connected.getOrNull().orEmpty()
            )

            if (connected.isSuccess) {
                diagnostics.record(ConnectionStage.CONNECT, "Connected using ${candidate.transport}")
                state = state.copy(
                    stage = ConnectionStage.CONNECT,
                    attempts = attempts,
                    selected = candidate.transport,
                    connected = true,
                    userMessage = "Connected"
                )
                onStateChanged(state)
                return Result.success(candidate.transport)
            }

            state = state.copy(attempts = attempts)
            onStateChanged(state)
        }

        diagnostics.record(ConnectionStage.FAILED, "All connection strategies failed")
        state = state.copy(
            stage = ConnectionStage.FAILED,
            connected = false,
            userMessage = "No compatible connection method succeeded"
        )
        onStateChanged(state)
        return Result.failure(IllegalStateException(state.userMessage))
    }

    fun diagnosticsSnapshot(): List<DiagnosticEvent> = diagnostics.snapshot()
}

class PairingRequiredException(
    val transport: ConnectionTransport,
    message: String
) : IllegalStateException(message)
