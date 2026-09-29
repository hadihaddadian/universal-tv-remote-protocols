package com.haddadian.universalremote

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.StringReader
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.io.ByteArrayInputStream
import android.util.Base64
import java.util.Date
import java.util.UUID

 data class AtvCertificate(
    val key: PrivateKey,
    val certificate: X509Certificate
)

/** Persists the Android TV client identity encrypted with Android Keystore. */
class AtvCertificateStore(context: Context) {
    private val secureStore = com.haddadian.universalremote.security.SecureSecretStore(context.applicationContext)

    fun load(): AtvCertificate? = runCatching {
        val keyPem = secureStore.get(KEY_PRIVATE) ?: return null
        val certPem = secureStore.get(KEY_CERTIFICATE) ?: return null
        val key = PEMParser(StringReader(keyPem)).use {
            JcaPEMKeyConverter().getKeyPair(it.readObject() as PEMKeyPair).private
        }
        val certificate = PEMParser(StringReader(certPem)).use {
            JcaX509CertificateConverter().getCertificate(it.readObject() as X509CertificateHolder)
        }
        AtvCertificate(key, certificate)
    }.getOrNull()

    fun getOrCreate(): AtvCertificate = load() ?: generateAndSave()

    fun loadServerCertificate(deviceId: String): X509Certificate? = runCatching {
        val encoded = secureStore.get(serverKey(deviceId)) ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
    }.getOrNull()

    fun saveServerCertificate(deviceId: String, certificate: X509Certificate) {
        secureStore.put(
            serverKey(deviceId),
            Base64.encodeToString(certificate.encoded, Base64.NO_WRAP)
        )
    }

    fun save(identity: AtvCertificate) {
        secureStore.put(KEY_PRIVATE, pem(identity.key))
        secureStore.put(KEY_CERTIFICATE, pem(identity.certificate))
    }

    private fun generateAndSave(): AtvCertificate {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val now = Date()
        val expiry = Date(now.time + 10L * 365 * 24 * 60 * 60 * 1000)
        val subject = X500Name("CN=Universal TV Remote ${UUID.randomUUID()}")
        val builder = X509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            expiry,
            subject,
            org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        return AtvCertificate(keyPair.private, certificate).also(::save)
    }

    private fun pem(value: Any): String = StringWriter().also { writer ->
        JcaPEMWriter(writer).use { it.writeObject(value) }
    }.toString()

    private fun serverKey(deviceId: String): String = "atv.server.certificate." +
        java.util.UUID.nameUUIDFromBytes(deviceId.toByteArray(Charsets.UTF_8)).toString()

    private companion object {
        const val KEY_PRIVATE = "atv.client.privateKey"
        const val KEY_CERTIFICATE = "atv.client.certificate"
    }
}
