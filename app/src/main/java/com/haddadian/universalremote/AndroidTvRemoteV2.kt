package com.haddadian.universalremote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Public Android TV Remote v2 facade.
 * Pairing and remote transport are implemented by dedicated engines so that
 * UI code does not depend on protocol framing details.
 */
enum class AndroidTvState {
    DISCONNECTED, DISCOVERED, PAIRING, PIN_REQUIRED, CONNECTING, READY, FAILED
}

/** Common Android TV key codes exposed by the public Remote v2 facade. */
enum class AtvKey(val code: Int) {
    BACK(4), HOME(3), UP(19), DOWN(20), LEFT(21), RIGHT(22), OK(23),
    POWER(26), VOLUME_DOWN(25), VOLUME_UP(24), MUTE(164), GUIDE(172), INPUT(178),
    PLAY_PAUSE(85), STOP(86), NEXT(87), PREVIOUS(88), REWIND(89), FAST_FORWARD(90)
}

data class AndroidTvIdentity(
    val ip: String,
    val model: String? = null,
    val vendor: String? = null,
    val remoteServicePort: Int = 6466,
    val pairingPort: Int = 6467
)

class AndroidTvRemoteV2(
    private val certificateStore: AtvCertificateStore,
    private val pairingEngine: AndroidTvPairingEngine = AndroidTvPairingEngine(certificateStore),
    private val remoteEngine: AndroidTvRemoteEngine = AndroidTvRemoteEngine(certificateStore)
) {
    var state: AndroidTvState = AndroidTvState.DISCONNECTED
        private set

    private var identity: AndroidTvIdentity? = null

    suspend fun discover(ip: String): AndroidTvIdentity? = withContext(Dispatchers.IO) {
        val service = tcpOpen(ip, 6466)
        val pairing = tcpOpen(ip, 6467)
        if (!service && !pairing) return@withContext null
        val result = AndroidTvIdentity(ip)
        identity = result
        state = AndroidTvState.DISCOVERED
        result
    }

    fun startPairing(id: AndroidTvIdentity): Result<Unit> {
        identity = id
        state = AndroidTvState.PAIRING
        return pairingEngine.start(id.ip, "androidtv:${id.ip}").map {
            state = AndroidTvState.PIN_REQUIRED
        }
    }

    fun finishPairing(pin: String): Result<Unit> {
        val id = identity ?: return Result.failure(IllegalStateException("No Android TV selected"))
        if (!pin.matches(Regex("(?i)[0-9a-f]{6}"))) {
            return Result.failure(IllegalArgumentException("Pairing code must contain 6 hexadecimal characters"))
        }
        state = AndroidTvState.CONNECTING
        return pairingEngine.finish(pin).mapCatching {
            remoteEngine.connect(id.ip, "androidtv:${id.ip}").getOrThrow()
            state = AndroidTvState.READY
        }.recoverCatching {
            state = AndroidTvState.FAILED
            throw it
        }
    }

    fun connectPaired(id: AndroidTvIdentity): Result<Unit> {
        identity = id
        state = AndroidTvState.CONNECTING
        return remoteEngine.connect(id.ip, "androidtv:${id.ip}").onSuccess { state = AndroidTvState.READY }
            .onFailure { state = AndroidTvState.FAILED }
    }

    fun sendKey(key: AtvKey): Result<Unit> = remoteEngine.sendKey(key.code)

    fun sendText(text: String): Result<Unit> = remoteEngine.sendText(text)

    fun setVolume(level: Int): Result<Unit> = remoteEngine.setVolume(level)

    fun close() {
        pairingEngine.close()
        remoteEngine.close()
        state = AndroidTvState.DISCONNECTED
    }

    private fun tcpOpen(ip: String, port: Int): Boolean = runCatching {
        Socket().use { socket -> socket.connect(InetSocketAddress(ip, port), 500) }
        true
    }.getOrDefault(false)
}
