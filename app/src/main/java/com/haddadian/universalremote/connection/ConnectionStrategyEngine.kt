package com.haddadian.universalremote.connection

class ConnectionStrategyEngine {
    fun rank(device: TvFingerprint, available: Set<ConnectionTransport>, protocolTransports: Set<ConnectionTransport>): List<ConnectionCandidate> {
        return available.mapNotNull { transport ->
            if (protocolTransports.isNotEmpty() && transport !in protocolTransports) return@mapNotNull null
            val score = when (transport) {
                ConnectionTransport.ANDROID_TV_REMOTE_V2 -> if (device.platform?.contains("android", true) == true) 100 else 70
                ConnectionTransport.BRAND_PROTOCOL -> if (!device.manufacturer.isNullOrBlank()) 90 else 50
                ConnectionTransport.MDNS -> 75
                ConnectionTransport.BLUETOOTH -> 65
                ConnectionTransport.WIFI_DIRECT -> 60
                ConnectionTransport.IR -> if (device.capabilities.contains("ir_blaster_available")) 55 else 0
                ConnectionTransport.ADB -> 40
                ConnectionTransport.EXTERNAL_BRIDGE -> 35
            }.takeIf { it > 0 } ?: return@mapNotNull null
            ConnectionCandidate(transport, score, "Strategy score $score")
        }.sortedByDescending { it.score }
    }
}
