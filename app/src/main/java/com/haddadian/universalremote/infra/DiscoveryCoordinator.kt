package com.haddadian.universalremote.infra

import android.content.Context
import com.haddadian.universalremote.AndroidTvDiscovery
import com.haddadian.universalremote.domain.TvDevice
import com.haddadian.universalremote.connection.protocol.ProtocolIds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest

/**
 * Canonical discovery entry point. Network discovery is started only after
 * the phone hardware/network preflight succeeds; IR remains local-hardware-only.
 */
class DiscoveryCoordinator(context: Context) {
    private val hardware = PhoneHardwareCapabilities(context.applicationContext)
    private val ssdp = DeviceDiscoveryService(context.applicationContext)
    private val androidTv = AndroidTvDiscovery(context.applicationContext)
    private val normalizer = DeviceDiscoveryNormalizer()

    suspend fun discover(): List<TvDevice> = coroutineScope {
        val ssdpJob = async {
            if (hardware.canRunWifiDiscovery()) {
                runCatching { ssdp.discover() }.getOrDefault(emptyList())
            } else emptyList()
        }
        val androidTvJob = async {
            if (!hardware.canRunWifiDiscovery()) return@async emptyList()
            withTimeoutOrNull(4_000L) {
                androidTv.services().take(20).toList()
            }.orEmpty().map { service ->
                TvDevice(
                    id = stableAndroidTvId(service.name, service.host, service.port),
                    name = service.name.ifBlank { "Android TV" },
                    address = service.host,
                    transport = "Wi‑Fi",
                    protocol = ProtocolIds.ANDROID_TV_REMOTE_V2,
                    stableIdentity = stableAndroidTvIdentity(service.name, service.host, service.port),
                    capabilities = setOf("navigation", "volume", "power", "mute", "keyboard", "app_launch", "playback"),
                    manufacturer = null,
                    model = service.name
                )
            }
        }
        normalizer.normalize((awaitAll(ssdpJob, androidTvJob).flatten()))
    }

    private fun stableAndroidTvId(name: String, host: String, port: Int): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(stableAndroidTvIdentity(name, host, port).toByteArray(Charsets.UTF_8))
            .take(10)
            .joinToString("") { "%02x".format(it) }
        return "androidtv-$digest"
    }

    private fun stableAndroidTvIdentity(name: String, host: String, port: Int): String {
        // Prefer a non-generic advertised service identity; for generic names, the
        // address remains necessary to avoid merging multiple identical TVs.
        val normalizedName = name.trim().lowercase()
        val genericName = normalizedName.isBlank() ||
            normalizedName in setOf("android tv", "androidtv", "google tv")
        return if (!genericName) {
            "androidtv|$normalizedName|$port"
        } else {
            "androidtv|$host|$port"
        }
    }
}
