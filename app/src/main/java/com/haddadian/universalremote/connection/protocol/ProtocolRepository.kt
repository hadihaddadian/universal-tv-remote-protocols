package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.TvFingerprint

interface ProtocolRepository {
    suspend fun findCompatible(manufacturer: String?, platform: String?, model: String?): ProtocolPackage?
    suspend fun get(id: ProtocolId, version: Int? = null): ProtocolPackage?
}

class CompositeProtocolRepository(private val repositories: List<ProtocolRepository>) : ProtocolRepository {
    override suspend fun findCompatible(manufacturer: String?, platform: String?, model: String?): ProtocolPackage? {
        for (repository in repositories) repository.findCompatible(manufacturer, platform, model)?.let { return it }
        return null
    }
    override suspend fun get(id: ProtocolId, version: Int?): ProtocolPackage? {
        for (repository in repositories) repository.get(id, version)?.let { return it }
        return null
    }
}

class InMemoryProtocolRepository(
    packages: Collection<ProtocolPackage>,
    private val selector: ProtocolSelector = ProtocolSelector()
) : ProtocolRepository {
    private val values = packages.associateBy { it.manifest.id }

    override suspend fun findCompatible(manufacturer: String?, platform: String?, model: String?): ProtocolPackage? {
        val fingerprint = TvFingerprint(manufacturer = manufacturer, platform = platform, model = model)
        val match = selector.select(fingerprint, values.values.map { it.profile }) ?: return null
        return values[match.profile.manifest.id]
    }

    override suspend fun get(id: ProtocolId, version: Int?): ProtocolPackage? =
        values[id]?.takeIf { version == null || it.manifest.version == version }
}
