package com.haddadian.universalremote.connection

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConnectionMemoryEntry(
    val fingerprint: String,
    val transport: ConnectionTransport,
    val successCount: Int = 0,
    val failureCount: Int = 0
)

class ConnectionMemory(context: Context) {
    private val prefs = context.getSharedPreferences("connection_memory", Context.MODE_PRIVATE)
    private val entries = mutableListOf<ConnectionMemoryEntry>()

    init { load() }

    fun preferred(fingerprint: String): List<ConnectionTransport> =
        entries
            .filter { it.fingerprint == fingerprint }
            .sortedByDescending { it.successCount - it.failureCount }
            .map { it.transport }

    fun record(fingerprint: String, transport: ConnectionTransport, success: Boolean) {
        val index = entries.indexOfFirst {
            it.fingerprint == fingerprint && it.transport == transport
        }

        if (index < 0) {
            entries += ConnectionMemoryEntry(
                fingerprint = fingerprint,
                transport = transport,
                successCount = if (success) 1 else 0,
                failureCount = if (success) 0 else 1
            )
        } else {
            val old = entries[index]
            entries[index] = old.copy(
                successCount = old.successCount + if (success) 1 else 0,
                failureCount = old.failureCount + if (success) 0 else 1
            )
        }

        // Keep the store small.
        if (entries.size > 100) {
            entries.subList(0, entries.size - 100).clear()
        }
        save()
    }

    private fun save() {
        val array = JSONArray()
        entries.forEach {
            array.put(
                JSONObject()
                    .put("fingerprint", it.fingerprint)
                    .put("transport", it.transport.name)
                    .put("success", it.successCount)
                    .put("failure", it.failureCount)
            )
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    private fun load() {
        entries.clear()
        val raw = prefs.getString("entries", null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                entries += ConnectionMemoryEntry(
                    fingerprint = o.getString("fingerprint"),
                    transport = ConnectionTransport.valueOf(o.getString("transport")),
                    successCount = o.optInt("success", 0),
                    failureCount = o.optInt("failure", 0)
                )
            }
        }.onFailure {
            prefs.edit().remove("entries").apply()
        }
    }
}
