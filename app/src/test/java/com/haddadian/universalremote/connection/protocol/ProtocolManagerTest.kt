package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.TvFingerprint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ProtocolManagerTest {
    @Test
    fun resolvesBuiltInProtocolForAndroidTv() = runBlocking {
        val dir = Files.createTempDirectory("protocols").toFile()
        val manager = ProtocolManager(
            builtInRepository = BuiltInProtocolCatalog.repository(),
            remoteRepository = null,
            store = FileProtocolStore(dir),
            validator = ProtocolPackageValidator(24, signatureVerifier = BuiltInProtocolTrust())
        )

        val result = manager.prepareFor(
            TvFingerprint(platform = "androidtv", manufacturer = "Android TV")
        )

        assertTrue(result is ProtocolManagerResult.Success)
        assertEquals("androidtv-remote-v2", (result as ProtocolManagerResult.Success).value.manifest.id.value)
    }

    @Test
    fun rejectsDowngrade() = runBlocking {
        val dir = Files.createTempDirectory("protocols").toFile()
        val store = FileProtocolStore(dir)
        val packageV1 = BuiltInProtocolCatalog.repository().get(ProtocolId("androidtv-remote-v2"))!!
        store.put(packageV1)

        val packageV0 = packageV1.copy(
            manifest = packageV1.manifest.copy(version = 0),
            profile = packageV1.profile.copy(manifest = packageV1.manifest.copy(version = 0))
        )
        val manager = ProtocolManager(
            builtInRepository = BuiltInProtocolCatalog.repository(),
            remoteRepository = null,
            store = store,
            validator = ProtocolPackageValidator(24, signatureVerifier = BuiltInProtocolTrust())
        )

        val result = manager.install(packageV0)
        assertTrue(result is ProtocolManagerResult.Failure)
    }

    @Test
    fun codecPreservesPayloadBytesForHashVerification() = runBlocking {
        val packageValue = BuiltInProtocolCatalog.repository().get(ProtocolId("sony-ircc"))!!
        val encoded = ProtocolPackageCodec.encode(packageValue)
        val decoded = ProtocolPackageCodec.decode(encoded)
        assertEquals(packageValue.manifest.payloadSha256, sha256Hex(decoded.payloadBytes))
        assertEquals(packageValue.manifest.id, decoded.manifest.id)
    }
}
