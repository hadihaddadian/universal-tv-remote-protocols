package com.haddadian.universalremote.security

data class SecurityAuditEntry(val timestampMs: Long, val category: String, val result: String)

class SecurityAuditLog(private val maxEntries: Int = 300) {
    private val entries = ArrayDeque<SecurityAuditEntry>()
    fun record(category: String, result: String) {
        if (entries.size >= maxEntries) entries.removeFirst()
        entries.addLast(SecurityAuditEntry(System.currentTimeMillis(), category, result))
    }
    fun snapshot(): List<SecurityAuditEntry> = entries.toList()
}
