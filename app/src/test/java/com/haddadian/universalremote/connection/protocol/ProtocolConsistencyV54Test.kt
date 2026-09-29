package com.haddadian.universalremote.connection.protocol

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolConsistencyV54Test {
    private val commandCapability = ProtocolCommandCapabilities.capabilityByCommand

    @Test
    fun everyCatalogCommandMatchesADeclaredCapability() {
        BuiltInProtocolCatalog.packages().forEach { pkg ->
            pkg.profile.commands.forEach { command ->
                val capability = commandCapability[command.command]
                assertTrue("Unknown command ${command.command}", capability != null)
                assertTrue(
                    "${pkg.manifest.id} declares ${command.command} without $capability",
                    capability in pkg.profile.capabilities
                )
            }
        }
    }

    @Test
    fun everyCatalogCapabilityHasItsRequiredCommands() {
        BuiltInProtocolCatalog.packages().forEach { pkg ->
            val commands = pkg.profile.commands.map { it.command }.toSet()
            pkg.profile.capabilities.forEach { capability ->
                val required = commandCapability.filterValues { it == capability }.keys
                required.forEach { command ->
                    assertTrue("${pkg.manifest.id}: $capability requires $command", command in commands)
                }
            }
        }
    }

    @Test
    fun genericIrIsNotSelectedForUnknownNetworkFingerprint() {
        val profiles = BuiltInProtocolCatalog.packages().map { it.profile }
        val match = ProtocolSelector().select(
            com.haddadian.universalremote.connection.TvFingerprint(
                manufacturer = "Unknown", platform = "unknown", address = "192.168.1.50"
            ), profiles
        )
        assertFalse(match?.profile?.manifest?.id?.value == "generic-ir-tv")
    }

    @Test
    fun genericIrRequiresExplicitIrFingerprint() {
        val profiles = BuiltInProtocolCatalog.packages().map { it.profile }
        val match = ProtocolSelector().select(
            com.haddadian.universalremote.connection.TvFingerprint(platform = "ir"), profiles
        )
        assertTrue(match?.profile?.manifest?.id?.value == "generic-ir-tv")
    }
}
