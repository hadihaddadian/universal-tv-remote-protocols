package com.haddadian.universalremote.architecture

import com.haddadian.universalremote.connection.ConnectionTransport
import com.haddadian.universalremote.connection.TvFingerprint
import com.haddadian.universalremote.connection.protocol.BuiltInProtocolCatalog
import com.haddadian.universalremote.connection.protocol.ProtocolBindingRegistry
import com.haddadian.universalremote.connection.protocol.ProtocolId
import com.haddadian.universalremote.connection.protocol.ProtocolSelector
import com.haddadian.universalremote.domain.Capability
import com.haddadian.universalremote.domain.EffectiveCapabilityResolver
import com.haddadian.universalremote.domain.toCapabilities
import com.haddadian.universalremote.infra.DeviceDiscoveryNormalizer
import com.haddadian.universalremote.domain.TvDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class V47HardeningTest {
    @Test fun androidTvBindingIsExecutable() {
        assertTrue(ProtocolBindingRegistry.fromBuiltIns().supports(ProtocolId("androidtv-remote-v2"), ConnectionTransport.ANDROID_TV_REMOTE_V2))
    }

    @Test fun selectorIsSharedAcrossRepositoryStyleInputs() {
        val profiles = BuiltInProtocolCatalog.packages().map { it.profile }
        val match = ProtocolSelector().select(TvFingerprint(manufacturer = "TCL", platform = "androidtv"), profiles)
        assertEquals("androidtv-remote-v2", match?.profile?.manifest?.id?.value)
    }

    @Test fun capabilityAliasesAreCanonicalized() {
        val result = EffectiveCapabilityResolver.resolveCanonical(setOf("navigation", "text", "volume"), setOf("dpad", "keyboard", "volume"))
        assertEquals(setOf(Capability.NAVIGATION, Capability.KEYBOARD, Capability.VOLUME), result)
    }

    @Test fun discoveryIdentityDoesNotDependOnDhcpAddressWhenDescriptiveDataExists() {
        val normalizer = DeviceDiscoveryNormalizer()
        val first = TvDevice("a", "Living Room TV", "192.168.1.10", "Wi-Fi", "androidtv-remote-v2", stableIdentity = "service:living-room", manufacturer = "Google", model = "Living Room")
        val second = first.copy(id = "b", address = "192.168.1.11")
        val result = normalizer.normalize(listOf(first, second))
        assertEquals(1, result.size)
        assertEquals("192.168.1.11", result.single().address)
    }
}
