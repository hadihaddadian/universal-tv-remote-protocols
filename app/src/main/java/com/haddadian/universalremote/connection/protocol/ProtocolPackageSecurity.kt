package com.haddadian.universalremote.connection.protocol

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.security.MessageDigest

interface ProtocolSignatureVerifier {
    fun verify(manifest: ProtocolPackageManifest, payload: ByteArray): Boolean
}

/**
 * Verifies ECDSA P-256/SHA-256 signatures. The public key is supplied by the
 * application build configuration or a trusted key rotation mechanism; it is
 * never downloaded from the same untrusted package.
 */
class EcdsaProtocolSignatureVerifier(
    private val publicKeyBase64: String
) : ProtocolSignatureVerifier {
    override fun verify(manifest: ProtocolPackageManifest, payload: ByteArray): Boolean {
        val signatureBase64 = manifest.signatureBase64 ?: return false
        return runCatching {
            val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
            val signatureBytes = Base64.getDecoder().decode(signatureBase64)
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey: PublicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey)
                update(canonicalSigningBytes(manifest, payload))
                verify(signatureBytes)
            }
        }.getOrDefault(false)
    }
}

class BuiltInProtocolTrust : ProtocolSignatureVerifier {
    override fun verify(manifest: ProtocolPackageManifest, payload: ByteArray): Boolean =
        manifest.trust == ProtocolTrust.BUILTIN
}

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

internal fun canonicalSigningBytes(manifest: ProtocolPackageManifest, payload: ByteArray): ByteArray =
    listOf(
        manifest.id.value,
        manifest.manufacturer,
        manifest.platform,
        manifest.version.toString(),
        manifest.minAppVersion.toString(),
        manifest.modelPatterns.sorted().joinToString(","),
        manifest.trust.name,
        manifest.source.name,
        sha256Hex(payload)
    ).joinToString("\n").toByteArray(Charsets.UTF_8)
