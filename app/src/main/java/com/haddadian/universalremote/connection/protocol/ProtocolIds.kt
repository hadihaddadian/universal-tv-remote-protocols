package com.haddadian.universalremote.connection.protocol

/** Canonical protocol package IDs shared by discovery, auth, storage and runtime. */
object ProtocolIds {
    const val ANDROID_TV_REMOTE_V2 = "androidtv-remote-v2"
    const val SAMSUNG_WS = "samsung-ws"
    const val LG_SSAP = "lg-ssap"
    const val SONY_IRCC = "sony-ircc"
    const val PHILIPS_JOINTSPACE = "philips-jointspace"
    const val ROKU_ECP = "roku-ecp"
    const val GENERIC_IR_TV = "generic-ir-tv"

    /** V53 used short discovery IDs; retain read compatibility for stored credentials. */
    fun credentialAliases(protocolId: String): List<String> = when (protocolId) {
        SAMSUNG_WS -> listOf(SAMSUNG_WS, "samsung")
        LG_SSAP -> listOf(LG_SSAP, "webos")
        SONY_IRCC -> listOf(SONY_IRCC, "sony")
        PHILIPS_JOINTSPACE -> listOf(PHILIPS_JOINTSPACE, "philips")
        ROKU_ECP -> listOf(ROKU_ECP, "roku")
        GENERIC_IR_TV -> listOf(GENERIC_IR_TV, "ir")
        else -> listOf(protocolId)
    }
}
