package com.haddadian.universalremote

import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

/**
 * TLS configuration for Android TV Remote v2.
 *
 * Pairing may use a temporary TOFU trust decision because the TV certificate
 * is not known before pairing. A paired connection must supply the stored
 * server certificate and is rejected on mismatch.
 */
object AtvTls {
    fun clientSocket(
        host: String,
        port: Int,
        identity: AtvCertificate,
        pinnedServer: X509Certificate? = null
    ): SSLSocket {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        keyStore.setKeyEntry("client", identity.key, CharArray(0), arrayOf(identity.certificate))

        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, CharArray(0))
        }.keyManagers

        val defaultTrust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }.trustManagers.filterIsInstance<X509TrustManager>().first()

        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrust.acceptedIssuers
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                defaultTrust.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if (pinnedServer == null) {
                    // Initial pairing is a deliberate TOFU operation. The caller
                    // must persist the peer certificate before treating pairing as complete.
                    require(chain.isNotEmpty()) { "TV did not provide a certificate" }
                    return
                }
                val peer = chain.firstOrNull() ?: throw SSLPeerUnverifiedException("Empty TV certificate chain")
                if (!pinnedServer.encoded.contentEquals(peer.encoded)) {
                    throw SSLPeerUnverifiedException("Pinned Android TV certificate mismatch")
                }
            }
        }

        val context = SSLContext.getInstance("TLS").apply {
            init(keyManagers, arrayOf(trustManager), null)
        }

        return (context.socketFactory.createSocket(host, port) as SSLSocket).apply {
            useClientMode = true
            startHandshake()
        }
    }
}
