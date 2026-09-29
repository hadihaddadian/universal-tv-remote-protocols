package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.ConnectionTransport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCapabilityConsistencyTest {
    @Test fun everyBuiltInProtocolHasAnExecutableBinding() {
        val registry = ProtocolBindingRegistry.fromBuiltIns()
        BuiltInProtocolCatalog.packages().forEach { pkg ->
            assertTrue("Missing executable binding for ${pkg.manifest.id}", pkg.profile.transports.any { registry.supports(pkg.manifest.id, it) })
        }
    }

    @Test fun unimplementedProtocolsAreNotCatalogued() {
        assertFalse(BuiltInProtocolCatalog.packages().any { it.manifest.id.value == "dlna-upnp-control" })
    }

    @Test fun genericIrIsBuiltInOnly() {
        val registry = ProtocolBindingRegistry.fromBuiltIns()
        assertTrue(registry.supports(ProtocolId("generic-ir-tv"), ConnectionTransport.IR))
    }

    @Test fun stagedVoiceIsNotAdvertisedByAndroidTvDiscoveryContract() {
        val android = BuiltInProtocolCatalog.packages().first { it.manifest.id.value == "androidtv-remote-v2" }
        assertFalse("voice" in android.profile.capabilities)
    }
}

class ProtocolCommandMetadataTest {
    @Test fun extendedCapabilitiesHaveCommandDefinitions() {
        BuiltInProtocolCatalog.packages().forEach { pkg ->
            val commands = pkg.profile.commands.map { it.command }.toSet()
            if ("playback" in pkg.profile.capabilities) assertTrue("PlayPause missing for ${pkg.manifest.id}", "PlayPause" in commands)
            if ("keyboard" in pkg.profile.capabilities) assertTrue("SendText missing for ${pkg.manifest.id}", "SendText" in commands)
            if ("app_launch" in pkg.profile.capabilities) assertTrue("LaunchApp missing for ${pkg.manifest.id}", "LaunchApp" in commands)
        }
    }
}
