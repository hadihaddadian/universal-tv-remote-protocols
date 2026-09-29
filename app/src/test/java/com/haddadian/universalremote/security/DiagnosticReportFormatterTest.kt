package com.haddadian.universalremote.security

import com.haddadian.universalremote.connection.ConnectionDiagnostics
import com.haddadian.universalremote.connection.DiagnosticEvent
import com.haddadian.universalremote.connection.ConnectionStage
import com.haddadian.universalremote.connection.TvFingerprint
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticReportFormatterTest {
    @Test fun reportRedactsSensitiveValues() {
        val report = DiagnosticReportFormatter.format(
            ConnectionDiagnostics(
                TvFingerprint(
                    name = "Living Room",
                    manufacturer = "Samsung",
                    model = "Model",
                    platform = "samsung",
                    address = "192.168.1.42",
                    port = 8002,
                    capabilities = emptySet()
                ),
                listOf(DiagnosticEvent(1, ConnectionStage.CONNECT, "token=secret-value"))
            )
        )
        assertTrue(report.contains("192.168.x.x"))
        assertFalse(report.contains("secret-value"))
        assertFalse(report.contains("192.168.1.42"))
    }
}
