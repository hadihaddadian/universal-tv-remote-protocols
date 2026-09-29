package com.haddadian.universalremote.ui

import com.haddadian.universalremote.connection.ConnectionDiagnostics

data class ConnectionDiagnosticsState(
    val diagnostics: ConnectionDiagnostics? = null,
    val running: Boolean = false
)
