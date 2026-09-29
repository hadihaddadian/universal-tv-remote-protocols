package com.haddadian.universalremote.infra

import com.haddadian.universalremote.domain.TvDevice

/** Merges duplicate observations while keeping identity independent of a DHCP address when possible. */
class DeviceDiscoveryNormalizer {
    fun normalize(devices: Collection<TvDevice>): List<TvDevice> = devices
        .groupBy { stableKey(it) }
        .values
        .map { candidates ->
            candidates.reduce { a, b ->
                a.copy(
                    address = b.address ?: a.address,
                    name = a.name.ifBlank { b.name },
                    manufacturer = a.manufacturer ?: b.manufacturer,
                    model = a.model ?: b.model,
                    firmware = a.firmware ?: b.firmware,
                    capabilities = a.capabilities + b.capabilities
                )
            }
        }
        .sortedBy { it.name.lowercase() }

    private fun stableKey(device: TvDevice): String {
        device.stableIdentity?.takeIf { it.isNotBlank() }?.let { return it }
        if (device.id == "generic-ir-tv") return "phone:ir-emitter"
        // Without a vendor/service identifier, IP remains the only safe way to
        // distinguish two identical TVs. We intentionally do not merge them
        // merely because their model/name match.
        return listOf(device.protocol, device.manufacturer, device.model, device.name, device.address)
            .joinToString("|") { it?.trim()?.lowercase() ?: "" }
    }
}
