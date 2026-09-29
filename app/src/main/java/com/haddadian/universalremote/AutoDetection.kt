package com.haddadian.universalremote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Model/fingerprint evidence produced by discovery. Protocol selection is owned by ProtocolManager. */
data class ModelIdentity(
    val manufacturer: String,
    val model: String?,
    val platform: String,
    val firmware: String?,
    val confidence: Int
)

class ModelDetector {
    suspend fun detect(device: TvDevice): ModelIdentity = withContext(Dispatchers.IO) {
        when (device.protocol.lowercase()) {
            "generic-ir-tv" -> ModelIdentity("Generic", "IR emitter", "ir", null, 100)
            "androidtv", "androidtv-remote-v2", "googletv" -> ModelIdentity("Google/Android TV", null, "androidtv", null, 95)
            "samsung", "samsung-ws" -> ModelIdentity("Samsung", null, "samsung", null, 90)
            "webos", "lg-ssap" -> ModelIdentity("LG", null, "webos", null, 90)
            "sony", "sony-ircc" -> ModelIdentity("Sony", null, "sony", null, 90)
            "roku", "roku-ecp" -> ModelIdentity("Roku", null, "roku", null, 95)
            "philips", "philips-jointspace" -> ModelIdentity("Philips", null, "philips", null, 85)
            "vidaa" -> ModelIdentity("Hisense", null, "vidaa", null, 80)
            else -> ModelIdentity("Unknown", null, device.protocol, null, 20)
        }
    }
}

object AndroidTvFingerprints {
    const val REMOTE_SERVICE = "_androidtvremote2._tcp"
    const val CAST = "_googlecast._tcp"
    const val PAIRING_PORT = 6467
    const val REMOTE_PORT = 6466
}
