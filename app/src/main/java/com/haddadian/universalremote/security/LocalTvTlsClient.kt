package com.haddadian.universalremote.security

import com.haddadian.universalremote.connection.TvTransportPolicy
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.util.concurrent.TimeUnit

/**
 * Client for local TVs that expose WSS with a self-signed certificate.
 * First use is TOFU; the peer certificate hash is then pinned per device.
 */
class LocalTvTlsClient(private val pinStore: DeviceCertificatePinStore) {
    fun create(host: String, deviceId: String): OkHttpClient {
        require(TvTransportPolicy.isPrivateIpv4Host(host)) { "Local TV TLS is restricted to private IPv4 addresses" }
        val pinned = pinStore.get(deviceId)
        if (pinned == null) {
            RemoteLog.security("First-use TLS TOFU for local TV device=$deviceId; certificate will be pinned after handshake")
        }
        val trustAll = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        }
        val ssl = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
        }
        val builder = OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trustAll)
            .hostnameVerifier(HostnameVerifier { name, _ -> TvTransportPolicy.isPrivateIpv4Host(name) })
        if (pinned != null) {
            builder.certificatePinner(CertificatePinner.Builder().add(host, "sha256/$pinned").build())
        }
        return builder
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}

class DeviceCertificatePinStore(context: android.content.Context) {
    private val secureStore = SecureSecretStore(context.applicationContext)
    fun get(deviceId: String): String? = secureStore.get("tv.tls.pin.$deviceId")
    fun put(deviceId: String, pin: String) {
        require(pin.isNotBlank())
        secureStore.put("tv.tls.pin.$deviceId", pin)
    }
    fun remove(deviceId: String) = secureStore.remove("tv.tls.pin.$deviceId")
}
