package com.haddadian.universalremote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.haddadian.universalremote.infra.PhoneHardwareCapabilities
import com.haddadian.universalremote.connection.TvTransportPolicy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DiscoveredAndroidTv(
    val name: String,
    val host: String,
    val port: Int,
    val serviceType: String
)

class AndroidTvDiscovery(context: Context) {
    private val hardware = PhoneHardwareCapabilities(context.applicationContext)
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun services(): Flow<DiscoveredAndroidTv> = callbackFlow {
        if (!hardware.canRunWifiDiscovery()) {
            close()
            return@callbackFlow
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, errorCode: Int) { close() }
            override fun onStopDiscoveryFailed(type: String, errorCode: Int) {}
            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                if (info.serviceType.contains("androidtvremote2", true) ||
                    info.serviceType.contains("androidtvremote", true)) {
                    nsd.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            val host = resolved.host?.hostAddress ?: return
                            if (!TvTransportPolicy.isPrivateIpv4Host(host)) return
                            trySend(DiscoveredAndroidTv(
                                resolved.serviceName, host, resolved.port, resolved.serviceType
                            ))
                        }
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                    })
                }
            }
        }
        nsd.discoverServices("_androidtvremote2._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { runCatching { nsd.stopServiceDiscovery(listener) } }
    }
}
