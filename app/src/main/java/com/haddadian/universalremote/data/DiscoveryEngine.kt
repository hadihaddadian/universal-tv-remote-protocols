package com.haddadian.universalremote.data

import com.haddadian.universalremote.domain.TvDevice

interface DiscoverySource {
    suspend fun discover(): List<TvDevice>
}

class TvDeviceDiscoveryAggregator(private val sources: List<DiscoverySource>) {
    suspend fun discover(): List<TvDevice> =
        sources.flatMap { runCatching { it.discover() }.getOrDefault(emptyList()) }
            .distinctBy { it.id }
}
