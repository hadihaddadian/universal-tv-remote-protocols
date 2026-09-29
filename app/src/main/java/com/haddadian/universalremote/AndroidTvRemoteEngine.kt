package com.haddadian.universalremote

import com.haddadian.universalremote.atv.proto.*
import java.io.DataInputStream
import java.io.DataOutputStream
import com.haddadian.universalremote.connection.TvTransportPolicy
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.*

data class AtvRemoteState(
    val powered: Boolean? = null,
    val volume: Int? = null,
    val maxVolume: Int? = null,
    val muted: Boolean? = null,
    val currentApp: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val softwareVersion: String? = null
)

class AndroidTvRemoteEngine(
    private val certificateStore: AtvCertificateStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private companion object { const val MAX_FRAME_BYTES = 4 * 1024 * 1024 }

    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    @Volatile var state: AtvRemoteState = AtvRemoteState()
        private set

    private var imeCounter = 0
    private var imeFieldCounter = 0
    private var textFieldStart = 0
    private var textFieldEnd = 0
    private var receiveJob: Job? = null

    fun connect(host: String, deviceId: String, pinnedServer: X509Certificate? = null): Result<Unit> = runCatching {
        require(TvTransportPolicy.isPrivateIpv4Host(host)) { "Android TV address must be a private IPv4 address" }
        require(deviceId.isNotBlank()) { "Android TV device identity is required" }
        val identity = certificateStore.getOrCreate()
        val trustedServer = pinnedServer ?: certificateStore.loadServerCertificate(deviceId)
            ?: error("Android TV is not paired; pair the device before connecting")
        close()
        socket = AtvTls.clientSocket(host, 6466, identity, trustedServer)
        input = DataInputStream(socket!!.inputStream)
        output = DataOutputStream(socket!!.outputStream)

        socket!!.soTimeout = 10_000
        send(RemoteMessage.newBuilder().setRemoteConfigure(
            RemoteConfigure.newBuilder()
                .setCode1(622)
                .setDeviceInfo(
                    RemoteDeviceInfo.newBuilder()
                        .setModel("Universal TV Remote")
                        .setVendor("Universal TV Remote")
                        .setUnknown1(1)
                        .setUnknown2("1")
                        .setPackageName("com.haddadian.universalremote")
                        .setAppVersion("0.8")
                        .build()
                )
                .build()
        ).build())
        awaitHandshakeResponse { it.hasRemoteConfigure() || it.hasRemoteError() }

        send(RemoteMessage.newBuilder().setRemoteSetActive(
            RemoteSetActive.newBuilder().setActive(622)
        ).build())
        awaitHandshakeResponse { it.hasRemoteSetActive() || it.hasRemoteError() }

        socket!!.soTimeout = 0
        receiveJob = scope.launch { receiveLoop() }
        Unit
    }

    fun sendKey(code: Int, direction: RemoteDirection = RemoteDirection.SHORT): Result<Unit> = runCatching {
        send(RemoteMessage.newBuilder().setRemoteKeyInject(
            RemoteKeyInject.newBuilder().setKeyCodeValue(code).setDirection(direction)
        ).build())
    }

    @Synchronized
    fun sendText(text: String): Result<Unit> = runCatching {
        require(text.isNotEmpty())
        require(text.length <= 4096) { "Text input is too long" }
        val edit = RemoteEditInfo.newBuilder()
            .setInsert(1)
            .setTextFieldStatus(
                RemoteImeObject.newBuilder()
                    .setStart(textFieldStart)
                    .setEnd(textFieldEnd)
                    .setValue(text)
                    .build()
            )
            .build()
        send(RemoteMessage.newBuilder().setRemoteImeBatchEdit(
            RemoteImeBatchEdit.newBuilder()
                .setImeCounter(imeCounter)
                .setFieldCounter(imeFieldCounter)
                .addEditInfo(edit)
                .build()
        ).build())
        imeCounter++
    }

    fun showKeyboard(show: Boolean): Result<Unit> = runCatching {
        if (!show) return@runCatching Unit
        val status = RemoteTextFieldStatus.newBuilder()
            .setCounterField(imeFieldCounter)
            .build()
        send(RemoteMessage.newBuilder().setRemoteImeShowRequest(
            RemoteImeShowRequest.newBuilder()
                .setRemoteTextFieldStatus(status)
                .build()
        ).build())
    }

    fun volumeDelta(delta: Int): Result<Unit> = runCatching {
        val key = if (delta >= 0) RemoteKeyCode.KEYCODE_VOLUME_UP_VALUE else RemoteKeyCode.KEYCODE_VOLUME_DOWN_VALUE
        val count = kotlin.math.abs(delta).coerceAtMost(20)
        repeat(count) {
            send(RemoteMessage.newBuilder().setRemoteKeyInject(
                RemoteKeyInject.newBuilder()
                    .setKeyCodeValue(key)
                    .setDirection(RemoteDirection.SHORT)
                    .build()
            ).build())
        }
    }

    fun setVolume(level: Int): Result<Unit> = runCatching {
        require(level in 0..100) { "Volume must be between 0 and 100" }
        send(RemoteMessage.newBuilder().setRemoteSetVolumeLevel(
            RemoteSetVolumeLevel.newBuilder().setVolumeLevel(level)
        ).build())
    }

    fun launchApp(link: String): Result<Unit> = runCatching {
        send(RemoteMessage.newBuilder().setRemoteAppLinkLaunchRequest(
            RemoteAppLinkLaunchRequest.newBuilder().setAppLink(link)
        ).build())
    }

    private fun awaitHandshakeResponse(predicate: (RemoteMessage) -> Boolean) {
        while (true) {
            val length = readVarint(input ?: error("Not connected"))
            if (length <= 0 || length > MAX_FRAME_BYTES) error("Invalid handshake message length")
            val payload = ByteArray(length)
            (input ?: error("Not connected")).readFully(payload)
            val message = RemoteMessage.parseFrom(payload)
            if (message.hasRemoteError()) error("Android TV rejected the remote handshake")
            if (predicate(message)) {
                applyIncomingState(message)
                return
            }
        }
    }

    private fun receiveLoop() {
        try {
            while (socket?.isClosed == false) {
                val n = readVarint(input ?: return)
                if (n <= 0 || n > MAX_FRAME_BYTES) error("Invalid message length")
                val bytes = ByteArray(n); input!!.readFully(bytes)
                val msg = RemoteMessage.parseFrom(bytes)
                applyIncomingState(msg)
            }
        } catch (_: Throwable) {
            close()
        }
    }

    private fun applyIncomingState(msg: RemoteMessage) {
        when {
            msg.hasRemotePingRequest() ->
                send(RemoteMessage.newBuilder().setRemotePingResponse(
                    RemotePingResponse.newBuilder().setVal1(msg.remotePingRequest.val1)
                ).build())

            msg.hasRemoteStart() ->
                state = state.copy(powered = msg.remoteStart.started)

            msg.hasRemoteSetVolumeLevel() -> {
                val volume = msg.remoteSetVolumeLevel
                state = state.copy(
                    volume = volume.volumeLevel,
                    maxVolume = volume.volumeMax,
                    muted = volume.volumeMuted
                )
            }

            msg.hasRemoteImeBatchEdit() -> {
                val edit = msg.remoteImeBatchEdit
                imeCounter = edit.imeCounter
                imeFieldCounter = edit.fieldCounter
            }

            msg.hasRemoteImeKeyInject() -> {
                val ime = msg.remoteImeKeyInject
                textFieldStart = ime.textFieldStatus?.start ?: textFieldStart
                textFieldEnd = ime.textFieldStatus?.end ?: textFieldEnd
                state = state.copy(currentApp = ime.appInfo?.appPackage)
            }

            msg.hasRemoteConfigure() -> {
                val info = msg.remoteConfigure.deviceInfo
                if (info != null) {
                    state = state.copy(
                        manufacturer = info.vendor.takeIf { it.isNotBlank() },
                        model = info.model.takeIf { it.isNotBlank() },
                        softwareVersion = info.appVersion.takeIf { it.isNotBlank() }
                    )
                }
            }
        }
    }

    internal fun sendRaw(msg: RemoteMessage) = send(msg)

    @Synchronized
    private fun send(msg: RemoteMessage) {
        val bytes = msg.toByteArray()
        val out = output ?: error("Not connected")
        writeVarint(out, bytes.size)
        out.write(bytes)
        out.flush()
    }

    private fun readVarint(input: DataInputStream): Int {
        var result=0; var shift=0
        while (shift < 32) {
            val b=input.readUnsignedByte()
            result = result or ((b and 0x7f) shl shift)
            if ((b and 0x80)==0) return result
            shift += 7
        }
        error("Invalid varint")
    }

    private fun writeVarint(out: DataOutputStream, value: Int) {
        var v=value
        while (true) {
            if ((v and 0x7f.inv())==0) { out.writeByte(v); return }
            out.writeByte((v and 0x7f) or 0x80); v=v ushr 7
        }
    }

    fun isConnected(): Boolean = socket?.let { it.isConnected && !it.isClosed && it.isInputShutdown.not() && it.isOutputShutdown.not() } == true

    fun close() {
        receiveJob?.cancel()
        receiveJob = null
        runCatching { socket?.close() }
        socket=null; input=null; output=null
        imeCounter = 0
        imeFieldCounter = 0
        textFieldStart = 0
        textFieldEnd = 0
    }
}
