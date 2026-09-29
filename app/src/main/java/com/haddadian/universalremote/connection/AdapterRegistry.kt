package com.haddadian.universalremote.connection

class AdapterRegistry(
    adapters: List<ConnectionAdapter>
) {
    private val byTransport = adapters.associateBy { it.transport }

    fun get(transport: ConnectionTransport): ConnectionAdapter? = byTransport[transport]

    fun all(): List<ConnectionAdapter> = byTransport.values.toList()
}
