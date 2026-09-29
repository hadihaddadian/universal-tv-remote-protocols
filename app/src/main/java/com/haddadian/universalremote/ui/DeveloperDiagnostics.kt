package com.haddadian.universalremote.ui

import com.haddadian.universalremote.domain.TvDevice

/** Developer-only snapshot; deliberately excludes secrets and raw credentials. */
data class DeveloperDiagnostics(
    val deviceId: String?, val deviceName: String?, val protocol: String?,
    val address: String?, val phase: String, val capabilities: Set<String>, val lastError: String?
) {
    companion object {
        fun from(state: RemoteUiState): DeveloperDiagnostics {
            val d: TvDevice? = state.selectedDevice
            return DeveloperDiagnostics(d?.id, d?.name, d?.protocol, d?.address, state.phase.toString(), d?.capabilities ?: emptySet(), state.lastError)
        }
    }
}
