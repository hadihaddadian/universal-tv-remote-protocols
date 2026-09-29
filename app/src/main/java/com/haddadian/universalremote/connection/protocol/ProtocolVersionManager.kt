package com.haddadian.universalremote.connection.protocol

class ProtocolVersionManager(private val store: ProtocolStore) {
    suspend fun canInstall(candidate: ProtocolPackage): Boolean {
        val current = store.get(candidate.manifest.id) ?: return true
        if (current.manifest.payloadSha256.equals(candidate.manifest.payloadSha256, true)) return false
        return candidate.manifest.version > current.manifest.version
    }
    suspend fun rollbackTo(id: ProtocolId, knownGood: ProtocolPackage): Boolean {
        val current = store.get(id) ?: return false
        if (current.manifest.id != knownGood.manifest.id) return false
        store.put(knownGood)
        return true
    }
}
