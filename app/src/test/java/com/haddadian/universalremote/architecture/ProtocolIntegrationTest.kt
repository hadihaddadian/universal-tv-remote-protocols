package com.haddadian.universalremote.architecture

import com.haddadian.universalremote.connection.protocol.BuiltInProtocolCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProtocolIntegrationTest {
    @Test fun multipleAndroidTvBrandsShareOneProtocolCore() {
        val pkg = BuiltInProtocolCatalog.packages().first { it.manifest.id.value == "androidtv-remote-v2" }
        val ids = pkg.profile.supportedManufacturers
        assertNotNull(ids.find { it == "Sony" })
        assertNotNull(ids.find { it == "TCL" })
        assertNotNull(ids.find { it == "Hisense" })
        assertEquals("androidtv-remote-v2", pkg.profile.manifest.id.value)
    }
}
