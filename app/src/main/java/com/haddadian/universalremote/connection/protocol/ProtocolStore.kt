package com.haddadian.universalremote.connection.protocol

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

interface ProtocolStore {
    suspend fun get(id: ProtocolId): ProtocolPackage?
    suspend fun put(packageValue: ProtocolPackage)
    suspend fun remove(id: ProtocolId)
    suspend fun list(): List<ProtocolPackage>
}

/** Atomic file store with corruption quarantine instead of silently discarding bad packages. */
class FileProtocolStore(private val directory: File) : ProtocolStore {
    private val quarantine = directory.resolve("quarantine")

    init {
        directory.mkdirs()
        quarantine.mkdirs()
    }

    override suspend fun get(id: ProtocolId): ProtocolPackage? {
        val file = directory.resolve("${id.value}.ppkg")
        return readSafely(file)
    }

    override suspend fun put(packageValue: ProtocolPackage) {
        val target = directory.resolve("${packageValue.manifest.id.value}.ppkg")
        val temp = directory.resolve(".${target.name}.${System.nanoTime()}.tmp")
        temp.writeBytes(ProtocolPackageCodec.encode(packageValue))
        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: Exception) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    override suspend fun remove(id: ProtocolId) {
        directory.resolve("${id.value}.ppkg").delete()
    }

    override suspend fun list(): List<ProtocolPackage> =
        directory.listFiles { file -> file.isFile && file.extension == "ppkg" }
            ?.mapNotNull(::readSafely)
            ?: emptyList()

    private fun readSafely(file: File): ProtocolPackage? {
        if (!file.exists()) return null
        return runCatching { ProtocolPackageCodec.decode(file.readBytes()) }
            .getOrElse {
                quarantineCorrupt(file)
                null
            }
    }

    private fun quarantineCorrupt(file: File) {
        runCatching {
            val target = quarantine.resolve("${file.name}.${System.currentTimeMillis()}.corrupt")
            Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
