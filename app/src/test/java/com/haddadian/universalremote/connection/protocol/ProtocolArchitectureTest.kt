package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.ConnectionTransport
import com.haddadian.universalremote.connection.TvFingerprint
import org.junit.Assert.*
import org.junit.Test

class ProtocolArchitectureTest {
    @Test fun builtInCatalogUsesSharedAndroidTvProtocol() {
        val packages = BuiltInProtocolCatalog.packages()
        val android = packages.first { it.manifest.id.value == "androidtv-remote-v2" }
        val manufacturers = android.profile.supportedManufacturers
        assertTrue(manufacturers.contains("Sony"))
        assertTrue(manufacturers.contains("TCL"))
        assertTrue(manufacturers.contains("Hisense"))
    }

    @Test fun bindingRegistryIsDerivedFromBuiltIns() {
        val registry = ProtocolBindingRegistry.fromBuiltIns()
        assertTrue(registry.supports(ProtocolId("androidtv-remote-v2"), ConnectionTransport.ANDROID_TV_REMOTE_V2))
    }

    @Test fun selectorDoesNotLetGenericOutrankSpecific() {
        val selector = ProtocolSelector()
        val packages = BuiltInProtocolCatalog.packages()
        val match = selector.select(
            TvFingerprint(manufacturer = "TCL", platform = "androidtv"),
            packages.map { it.profile }
        )
        assertNotNull(match)
        assertEquals("androidtv-remote-v2", match!!.profile.manifest.id.value)
    }

    @Test fun packageHashMatchesManifest() {
        BuiltInProtocolCatalog.packages().forEach { pkg ->
            assertEquals(pkg.manifest.payloadSha256, sha256Hex(pkg.payloadBytes))
        }
    }
}


class ProtocolResolutionPolicyTest {
    @Test fun builtInResolutionDoesNotNeedRemoteRepository() = kotlinx.coroutines.runBlocking {
        var remoteCalls = 0
        val remote = object : ProtocolRepository {
            override suspend fun findCompatible(manufacturer: String?, platform: String?, model: String?): ProtocolPackage? {
                remoteCalls++
                return null
            }
            override suspend fun get(id: ProtocolId, version: Int?): ProtocolPackage? {
                remoteCalls++
                return null
            }
        }
        val validator = ProtocolPackageValidator(
            appVersion = 24,
            signatureVerifier = BuiltInProtocolTrust()
        )
        val manager = ProtocolManager(
            builtInRepository = BuiltInProtocolCatalog.repository(),
            remoteRepository = remote,
            store = object : ProtocolStore {
                private val values = mutableMapOf<ProtocolId, ProtocolPackage>()
                override suspend fun get(id: ProtocolId) = values[id]
                override suspend fun put(packageValue: ProtocolPackage) { values[packageValue.manifest.id] = packageValue }
                override suspend fun remove(id: ProtocolId) { values.remove(id) }
                override suspend fun list() = values.values.toList()
            },
            validator = validator
        )

        val result = manager.prepareFor(
            com.haddadian.universalremote.connection.TvFingerprint(
                manufacturer = "TCL", platform = "androidtv"
            )
        )
        assertTrue(result is ProtocolManagerResult.Success)
        assertEquals(0, remoteCalls)
    }
}
