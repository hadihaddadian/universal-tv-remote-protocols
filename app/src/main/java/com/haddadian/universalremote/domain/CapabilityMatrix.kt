package com.haddadian.universalremote.domain

/**
 * Compatibility capability matrix for legacy planners. The executable
 * ProtocolPackage catalog is authoritative; this projection intentionally
 * mirrors only capabilities currently implemented by the transports.
 */
data class ProtocolDescriptor(
    val id: String,
    val manufacturer: String,
    val transports: Set<ProfileTransport>,
    val capabilities: Set<TvCapability>,
    val requiresPairing: Boolean
)

object CapabilityMatrix {
    val protocols = listOf(
        ProtocolDescriptor(
            "androidtv-remote-v2", "Android TV",
            setOf(ProfileTransport.TCP),
            setOf(TvCapability.POWER, TvCapability.NAVIGATION, TvCapability.VOLUME, TvCapability.MUTE,
                TvCapability.KEYBOARD, TvCapability.APP_LAUNCH, TvCapability.PLAYBACK),
            true
        ),
        ProtocolDescriptor(
            "samsung-ws", "Samsung", setOf(ProfileTransport.TCP),
            setOf(TvCapability.POWER, TvCapability.NAVIGATION, TvCapability.VOLUME, TvCapability.MUTE), true
        ),
        ProtocolDescriptor(
            "lg-ssap", "LG", setOf(ProfileTransport.LOCAL_HTTP),
            setOf(TvCapability.POWER, TvCapability.VOLUME, TvCapability.MUTE), true
        ),
        ProtocolDescriptor(
            "sony-ircc", "Sony", setOf(ProfileTransport.LOCAL_HTTP),
            setOf(TvCapability.POWER, TvCapability.NAVIGATION, TvCapability.VOLUME, TvCapability.MUTE), true
        ),
        ProtocolDescriptor(
            "philips-jointspace", "Philips", setOf(ProfileTransport.LOCAL_HTTP),
            setOf(TvCapability.POWER, TvCapability.NAVIGATION, TvCapability.VOLUME, TvCapability.MUTE), false
        ),
        ProtocolDescriptor(
            "roku-ecp", "Roku", setOf(ProfileTransport.LOCAL_HTTP),
            setOf(TvCapability.POWER, TvCapability.NAVIGATION, TvCapability.VOLUME, TvCapability.MUTE), false
        ),
        ProtocolDescriptor(
            "generic-ir-tv", "Generic", setOf(ProfileTransport.INFRARED),
            setOf(TvCapability.POWER, TvCapability.NAVIGATION, TvCapability.VOLUME, TvCapability.MUTE), false
        )
    )

    fun resolve(device: ProfileDevice): ProtocolDescriptor? =
        protocols.firstOrNull { it.id.equals(device.id, true) ||
            it.manufacturer.equals(device.manufacturer, true) }

    fun capabilities(device: ProfileDevice): Set<TvCapability> =
        resolve(device)?.capabilities ?: device.capabilities
}
