package com.haddadian.universalremote.connection

data class DiscoveredTv(
    val fingerprint: TvFingerprint,
    val services: Set<String> = emptySet(),
    val discoverySource: String = "unknown",
    val confidence: Float = 0f
)

interface DiscoveryEngine {
    suspend fun discover(): List<DiscoveredTv>
}

class CompositeDiscoveryEngine(
    private val engines: List<DiscoveryEngine>
) : DiscoveryEngine {
    override suspend fun discover(): List<DiscoveredTv> {
        val all = mutableListOf<DiscoveredTv>()
        engines.forEach { engine ->
            runCatching { all += engine.discover() }
        }
        return all
            .groupBy { it.fingerprint.address ?: it.fingerprint.name ?: "unknown" }
            .map { (_, items) ->
                items.maxBy { it.confidence }
            }
    }
}
