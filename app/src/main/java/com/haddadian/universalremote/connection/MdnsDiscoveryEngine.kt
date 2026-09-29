package com.haddadian.universalremote.connection

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.haddadian.universalremote.infra.PhoneHardwareCapabilities
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Optional legacy mDNS discovery engine. It only discovers protocols that have
 * an executable runtime binding; unsupported Google Cast/AirPlay advertisements
 * are deliberately not surfaced as TV candidates.
 */
class MdnsDiscoveryEngine(context: Context) : DiscoveryEngine {
    private val hardware = PhoneHardwareCapabilities(context.applicationContext)
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    override suspend fun discover(): List<DiscoveredTv> {
        if (!hardware.canRunWifiDiscovery()) return emptyList()
        return discoverType("_androidtvremote2._tcp.")
            .distinctBy { "${it.fingerprint.address}:${it.fingerprint.port}" }
    }

    private suspend fun discoverType(type: String): List<DiscoveredTv> =
        withTimeoutOrNull(4_000L) {
            suspendCancellableCoroutine { cont ->
                val found = mutableListOf<DiscoveredTv>()
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(serviceType: String) = Unit
                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                            override fun onServiceResolved(info: NsdServiceInfo) {
                                val host = info.host?.hostAddress ?: return
                                found += DiscoveredTv(
                                    fingerprint = TvFingerprint(
                                        name = info.serviceName,
                                        address = host,
                                        port = info.port,
                                        platform = "Android TV",
                                        capabilities = setOf("mdns", "android_tv_remote_v2")
                                    ),
                                    services = setOf(type),
                                    discoverySource = "mDNS",
                                    confidence = .95f
                                )
                            }
                        })
                    }
                    override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
                    override fun onDiscoveryStopped(serviceType: String) {
                        if (cont.isActive) cont.resume(found)
                    }
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        if (cont.isActive) cont.resume(found)
                    }
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                }
                nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                cont.invokeOnCancellation { runCatching { nsd.stopServiceDiscovery(listener) } }
            }
        }.orEmpty()
}
