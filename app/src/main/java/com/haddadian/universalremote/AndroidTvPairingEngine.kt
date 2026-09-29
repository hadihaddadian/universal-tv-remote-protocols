package com.haddadian.universalremote

import com.haddadian.universalremote.atv.proto.*
import com.haddadian.universalremote.connection.TvTransportPolicy
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

data class AtvPairingResult(
    val certificate: X509Certificate,
    val privateKeyPem: String
)

class AndroidTvPairingEngine(
    private val certificateStore: AtvCertificateStore
) {
    private companion object {
        const val MAX_FRAME_BYTES = 64 * 1024
    }
    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var serverCertificate: X509Certificate? = null
    private var deviceId: String? = null

    fun start(host: String, deviceId: String, serviceName: String = "androidtv-remote"): Result<Unit> = runCatching {
        close()
        require(TvTransportPolicy.isPrivateIpv4Host(host)) { "Android TV address must be a private IPv4 address" }
        require(deviceId.isNotBlank()) { "Android TV device identity is required" }
        this.deviceId = deviceId
        val identity = certificateStore.getOrCreate()
        socket = AtvTls.clientSocket(host, 6467, identity)
        socket!!.soTimeout = 15_000
        input = DataInputStream(socket!!.inputStream)
        output = DataOutputStream(socket!!.outputStream)
        serverCertificate = socket!!.session.peerCertificates.first() as X509Certificate

        send(PairingMessage.newBuilder()
            .setProtocolVersion(2)
            .setStatus(PairingMessage.Status.STATUS_OK)
            .setPairingRequest(
                PairingRequest.newBuilder()
                    .setServiceName(serviceName)
                    .setClientName("Universal TV Remote")
                    .build()
            ).build())

        readUntil { it.hasPairingRequestAck() }

        // PairingOption is a client message. It declares the encodings and role
        // this client supports; the TV echoes it back as the acknowledgement.
        val pairingOption = PairingOption.newBuilder()
            .addInputEncodings(
                PairingEncoding.newBuilder()
                    .setType(EncodingType.ENCODING_TYPE_HEXADECIMAL)
                    .setSymbolLength(6)
                    .build()
            )
            .setPreferredRole(RoleType.ROLE_TYPE_INPUT)
            .build()
        send(
            PairingMessage.newBuilder()
                .setProtocolVersion(2)
                .setStatus(PairingMessage.Status.STATUS_OK)
                .setPairingOption(pairingOption)
                .build()
        )
        readUntil { it.hasPairingOption() }

        // The client selects the encoding/role after the TV acknowledges the
        // option. The TV then answers with PairingConfigurationAck.
        send(PairingMessage.newBuilder()
            .setProtocolVersion(2)
            .setStatus(PairingMessage.Status.STATUS_OK)
            .setPairingConfiguration(
                PairingConfiguration.newBuilder()
                    .setEncoding(
                        PairingEncoding.newBuilder()
                            .setType(EncodingType.ENCODING_TYPE_HEXADECIMAL)
                            .setSymbolLength(6)
                            .build()
                    )
                    .setClientRole(RoleType.ROLE_TYPE_INPUT)
                    .build()
            )
            .build())
        readUntil { it.hasPairingConfigurationAck() }
    }

    fun finish(code: String): Result<Unit> = runCatching {
        require(code.matches(Regex("(?i)[0-9a-f]{6}"))) { "Pairing code must be 6 hexadecimal characters" }
        val clientCert = certificateStore.getOrCreate().certificate
        val serverCert = serverCertificate ?: error("Pairing has not started")

        val clientKey = clientCert.publicKey as java.security.interfaces.RSAPublicKey
        val serverKey = serverCert.publicKey as java.security.interfaces.RSAPublicKey

        fun hexUnsigned(n: BigInteger): String = n.toString(16).padStart(2, '0')
        val input = (
            hexUnsigned(clientKey.modulus) +
            hexUnsigned(clientKey.publicExponent) +
            hexUnsigned(serverKey.modulus) +
            hexUnsigned(serverKey.publicExponent) +
            code.takeLast(4)
        ).toByteArray(Charsets.US_ASCII)

        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        require(digest[0].toInt() and 0xff == code.take(2).toInt(16)) {
            "Pairing code verification failed"
        }

        send(PairingMessage.newBuilder()
            .setProtocolVersion(2)
            .setStatus(PairingMessage.Status.STATUS_OK)
            .setPairingSecret(PairingSecret.newBuilder().setSecret(com.google.protobuf.ByteString.copyFrom(digest)).build())
            .build())

        val ack = readUntil { it.hasPairingSecretAck() }
        check(ack.status == PairingMessage.Status.STATUS_OK) { "TV rejected pairing" }
        certificateStore.saveServerCertificate(deviceId ?: error("Device identity missing"), serverCert)
        close()
    }

    private fun send(msg: PairingMessage) {
        val bytes = msg.toByteArray()
        val out = output ?: error("Not connected")
        writeVarint(out, bytes.size)
        out.write(bytes); out.flush()
    }

    private fun readUntil(predicate: (PairingMessage) -> Boolean): PairingMessage {
        while (true) {
            val n = readVarint(input ?: error("Not connected"))
            require(n in 1..MAX_FRAME_BYTES) { "Invalid pairing message length" }
            val bytes = ByteArray(n)
            (input ?: error("Not connected")).readFully(bytes)
            val msg = PairingMessage.parseFrom(bytes)
            if (msg.status != PairingMessage.Status.STATUS_OK && msg.status != PairingMessage.Status.UNKNOWN)
                error("TV pairing error: ${msg.status}")
            if (predicate(msg)) return msg
        }
    }

    private fun readVarint(input: DataInputStream): Int {
        var result = 0; var shift = 0
        while (shift < 32) {
            val b = input.readUnsignedByte()
            result = result or ((b and 0x7f) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        error("Invalid varint")
    }

    private fun writeVarint(out: DataOutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7f.inv()) == 0) { out.writeByte(v); return }
            out.writeByte((v and 0x7f) or 0x80); v = v ushr 7
        }
    }

    fun close() {
        runCatching { input?.close() }; runCatching { output?.close() }; runCatching { socket?.close() }
        input = null; output = null; socket = null; serverCertificate = null; deviceId = null
    }
}
