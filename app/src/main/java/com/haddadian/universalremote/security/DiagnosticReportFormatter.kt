package com.haddadian.universalremote.security

import com.haddadian.universalremote.connection.ConnectionDiagnostics

/** Builds a shareable diagnostic report without credentials, tokens or certificate material. */
object DiagnosticReportFormatter {
    fun format(diagnostics: ConnectionDiagnostics): String = buildString {
        appendLine("Universal TV Remote diagnostics")
        appendLine("Device: ${safe(diagnostics.device.name)}")
        appendLine("Manufacturer: ${safe(diagnostics.device.manufacturer)}")
        appendLine("Model: ${safe(diagnostics.device.model)}")
        appendLine("Platform: ${safe(diagnostics.device.platform)}")
        appendLine("Address: ${redactAddress(diagnostics.device.address)}")
        appendLine("DurationMs: ${diagnostics.durationMs}")
        appendLine("Events:")
        diagnostics.events.forEach { event ->
            appendLine("- ${event.timestampMs} | ${event.stage} | ${safe(event.message)}")
        }
    }

    private fun safe(value: String?): String = value.orEmpty()
        .replace(Regex("(?i)(pin|token|psk|password|secret|authorization)\\s*[:=]\\s*\\S+"), "$1=[REDACTED]")

    private fun redactAddress(address: String?): String {
        val value = address.orEmpty()
        val host = value.substringBefore(':')
        return if (host.count { it == '.' } == 3) {
            val parts = host.split('.')
            "${parts[0]}.${parts[1]}.x.x"
        } else "[REDACTED]"
    }
}
