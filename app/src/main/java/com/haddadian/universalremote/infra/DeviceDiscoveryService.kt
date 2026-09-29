package com.haddadian.universalremote.infra

import android.content.Context
import android.net.wifi.WifiManager
import com.haddadian.universalremote.domain.TvDevice
import com.haddadian.universalremote.connection.protocol.ProtocolIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/** SSDP/vendor discovery for TVs reachable on the active Wi-Fi network. */
class DeviceDiscoveryService(private val context: Context) {
    private val hardware = PhoneHardwareCapabilities(context)
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val ir = InfraredController(context)
    private val normalizer = DeviceDiscoveryNormalizer()

    suspend fun discover(): List<TvDevice> = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, TvDevice>()
        if (hardware.canRunWifiDiscovery()) runCatching {
            val multicastLock = wifiManager?.createMulticastLock("UniversalTVRemote.SSDP")?.apply {
                setReferenceCounted(true)
                acquire()
            }
            try {
                DatagramSocket().use { socket ->
                    socket.soTimeout = 450
                    val query = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST:239.255.255.250:1900\r\n" +
                        "MAN:\"ssdp:discover\"\r\n" +
                        "ST:ssdp:all\r\nMX:1\r\n\r\n"
                    val target = InetAddress.getByName("239.255.255.250")
                    socket.send(DatagramPacket(query.toByteArray(), query.length, target, 1900))
                    repeat(16) {
                        runCatching {
                            val buffer = ByteArray(4096)
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            classify(String(packet.data, 0, packet.length), packet.address.hostAddress)
                        }.getOrNull()?.let { device -> found[device.id] = device }
                    }
                }
            } finally {
                runCatching { if (multicastLock?.isHeld == true) multicastLock.release() }
            }
        }

        // IR is local hardware discovery, independent of Wi-Fi availability.
        if (hardware.hasIrEmitter() && ir.isAvailable()) {
            found[ProtocolIds.GENERIC_IR_TV] = TvDevice(
                id = ProtocolIds.GENERIC_IR_TV,
                name = "مادون قرمز موبایل",
                address = null,
                transport = "IR",
                protocol = ProtocolIds.GENERIC_IR_TV,
                stableIdentity = "phone:ir-emitter",
                capabilities = setOf("dpad", "navigation", "volume", "power", "mute"),
                manufacturer = "Generic",
                model = "IR emitter"
            )
        }
        normalizer.normalize(found.values)
    }

    private fun classify(response: String, ip: String?): TvDevice? {
        val address = ip ?: return null
        return when {
            response.contains("roku", true) -> TvDevice(
                id = "roku-$address", name = "Roku TV", address = address, transport = "Wi‑Fi", protocol = ProtocolIds.ROKU_ECP,
                capabilities = setOf("dpad", "navigation", "volume", "power", "mute"), manufacturer = "Roku"
            )
            response.contains("webos", true) || response.contains("lg", true) -> TvDevice(
                id = "lg-$address", name = "LG webOS", address = address, transport = "Wi‑Fi", protocol = ProtocolIds.LG_SSAP,
                capabilities = setOf("volume", "power", "mute"), manufacturer = "LG"
            )
            response.contains("samsung", true) -> TvDevice(
                id = "samsung-$address", name = "Samsung Smart TV", address = address, transport = "Wi‑Fi", protocol = ProtocolIds.SAMSUNG_WS,
                capabilities = setOf("dpad", "navigation", "volume", "power", "mute"), manufacturer = "Samsung"
            )
            response.contains("sony", true) -> TvDevice(
                id = "sony-$address", name = "Sony BRAVIA", address = address, transport = "Wi‑Fi", protocol = ProtocolIds.SONY_IRCC,
                capabilities = setOf("dpad", "navigation", "volume", "power", "mute"), manufacturer = "Sony"
            )
            response.contains("philips", true) -> TvDevice(
                id = "philips-$address", name = "Philips TV", address = address, transport = "Wi‑Fi", protocol = ProtocolIds.PHILIPS_JOINTSPACE,
                capabilities = setOf("dpad", "navigation", "volume", "power", "mute"), manufacturer = "Philips"
            )
            else -> null
        }
    }
}
