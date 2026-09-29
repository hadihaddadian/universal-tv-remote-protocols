package com.haddadian.universalremote.connection

interface ConnectionAiPlanner {
    suspend fun rank(
        device: TvFingerprint,
        candidates: List<ConnectionCandidate>,
        history: List<ConnectionAttempt>
    ): List<ConnectionCandidate>
}

class RuleBasedConnectionAiPlanner(
    private val memory: ConnectionMemory? = null
) : ConnectionAiPlanner {
    override suspend fun rank(
        device: TvFingerprint,
        candidates: List<ConnectionCandidate>,
        history: List<ConnectionAttempt>
    ): List<ConnectionCandidate> {
        val failed = history.filterNot { it.success }.map { it.transport }.toSet()
        val remembered = memory?.preferred(fingerprint(device)).orEmpty()

        return candidates
            .filterNot { it.transport in failed }
            .sortedWith(
                compareByDescending<ConnectionCandidate> {
                    val index = remembered.indexOf(it.transport)
                    if (index < 0) 0 else 1000 - index
                }.thenByDescending { it.score }
            )
    }

    private fun fingerprint(device: TvFingerprint): String =
        listOf(
            device.manufacturer,
            device.model,
            device.platform,
            device.address
        ).joinToString("|")
}
