package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.ConnectionTransport

/** APK-bundled, declarative protocol catalog. */
object BuiltInProtocolCatalog {
    fun packages(): List<ProtocolPackage> = listOf(
        profile(ProtocolIds.ANDROID_TV_REMOTE_V2, "Android TV", "androidtv", setOf(ConnectionTransport.ANDROID_TV_REMOTE_V2), "android_tv_remote_v2_pairing", setOf("Sony", "TCL", "Hisense", "Philips", "Xiaomi", "Sharp", "Skyworth"), setOf("_androidtvremote2._tcp", "tcp:6466", "tcp:6467"), setOf("power", "navigation", "volume", "mute", "keyboard", "app_launch", "playback", "input", "number_pad")),
        profile(ProtocolIds.SAMSUNG_WS, "Samsung", "samsung", setOf(ConnectionTransport.BRAND_PROTOCOL), "protocol-managed", emptySet(), setOf("samsung"), setOf("power", "navigation", "volume", "mute", "input", "number_pad", "playback")),
        profile(ProtocolIds.LG_SSAP, "LG", "webos", setOf(ConnectionTransport.BRAND_PROTOCOL), "protocol-managed-registration", emptySet(), setOf("webos"), setOf("power", "volume", "mute")),
        profile(ProtocolIds.SONY_IRCC, "Sony", "sony", setOf(ConnectionTransport.BRAND_PROTOCOL), "psk", emptySet(), setOf("sony"), setOf("power", "navigation", "volume", "mute")),
        profile(ProtocolIds.PHILIPS_JOINTSPACE, "Philips", "philips", setOf(ConnectionTransport.BRAND_PROTOCOL), "none", emptySet(), setOf("jointspace"), setOf("power", "navigation", "volume", "mute", "input", "number_pad", "playback")),
        profile(ProtocolIds.ROKU_ECP, "Roku", "roku", setOf(ConnectionTransport.BRAND_PROTOCOL), "none", emptySet(), setOf("roku:ecp"), setOf("power", "navigation", "volume", "mute", "input", "number_pad", "playback")),
        profile(ProtocolIds.GENERIC_IR_TV, "Generic", "ir", setOf(ConnectionTransport.IR), "none", emptySet(), setOf("ir"), setOf("power", "navigation", "volume", "mute", "input", "number_pad", "playback")),
    )

    fun repository(): ProtocolRepository = InMemoryProtocolRepository(packages())

    private fun profile(
        id: String,
        manufacturer: String,
        platform: String,
        transports: Set<ConnectionTransport>,
        auth: String,
        supportedManufacturers: Set<String>,
        discovery: Set<String>,
        capabilities: Set<String>
    ): ProtocolPackage {
        val commands = buildList {
            if ("power" in capabilities) add("Power")
            if ("navigation" in capabilities) {
                addAll(listOf("Home", "Back", "Up", "Down", "Left", "Right", "Ok"))
            }
            if ("volume" in capabilities) addAll(listOf("VolumeUp", "VolumeDown"))
            if ("mute" in capabilities) add("Mute")
            if ("playback" in capabilities) addAll(listOf("PlayPause", "Stop", "Previous", "Next", "Rewind", "FastForward"))
            if ("input" in capabilities) add("Input")
            if ("number_pad" in capabilities) add("Number")
            if ("navigation" in capabilities) add("Guide")
            if ("keyboard" in capabilities) add("SendText")
            if ("app_launch" in capabilities) add("LaunchApp")
        }.distinct().map { ProtocolCommandDefinition(it, it) }
        val manifest = ProtocolPackageManifest(
            id = ProtocolId(id), manufacturer = manufacturer, platform = platform,
            version = 1, minAppVersion = 24, modelPatterns = emptySet(),
            payloadSha256 = "0".repeat(64), signatureBase64 = null,
            trust = ProtocolTrust.BUILTIN, source = ProtocolSource.BUILTIN
        )
        val profile = ProtocolProfile(
            manifest = manifest,
            supportedManufacturers = supportedManufacturers,
            discoveryHints = discovery,
            transports = transports,
            authMode = auth,
            commands = commands,
            capabilities = capabilities
        )
        val payload = ProtocolPackageCodec.encodePayload(profile)
        val finalManifest = manifest.copy(payloadSha256 = sha256Hex(payload))
        return ProtocolPackage(finalManifest, profile.copy(manifest = finalManifest), payload)
    }
}
