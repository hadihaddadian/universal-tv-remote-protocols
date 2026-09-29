package com.haddadian.universalremote.connection.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtocolRuntimeArchitectureTest {
    @Test fun androidTvBrandsShareProtocolCore() {
        val pkg = BuiltInProtocolCatalog.packages().first { it.manifest.id.value == "androidtv-remote-v2" }
        assertEquals("androidtv-remote-v2", pkg.profile.manifest.id.value)
        assertTrue("Sony" in pkg.profile.supportedManufacturers)
        assertTrue("TCL" in pkg.profile.supportedManufacturers)
        assertTrue("Hisense" in pkg.profile.supportedManufacturers)
    }

    @Test fun builtInBindingsAreAvailable() {
        assertTrue(ProtocolBindingRegistry.fromBuiltIns().all().isNotEmpty())
    }
}
