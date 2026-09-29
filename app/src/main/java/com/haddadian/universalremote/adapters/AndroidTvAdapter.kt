package com.haddadian.universalremote.adapters

import android.content.Context
import com.haddadian.universalremote.AndroidTvDiscovery
import com.haddadian.universalremote.AndroidTvRemoteEngine
import com.haddadian.universalremote.AtvCertificateStore
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull

class AndroidTvAdapter(context: Context) : ProtocolAdapter {
    override val id = "androidtv-remote-v2"
    override val platform = "androidtv"

    private val discovery = AndroidTvDiscovery(context.applicationContext)
    private val engine = AndroidTvRemoteEngine(AtvCertificateStore(context.applicationContext))

    override suspend fun discover(): List<DeviceCandidate> {
        val services = withTimeoutOrNull(4_000L) {
            discovery.services().take(10).toList()
        }.orEmpty()

        return services.distinctBy { "${it.host}:${it.port}" }.map { service ->
            DeviceCandidate(
                id = "androidtv-${service.host}:${service.port}",
                host = service.host,
                port = service.port,
                manufacturer = null,
                model = service.name,
                platform = platform,
                confidence = 100
            )
        }
    }

    override suspend fun connect(device: DeviceCandidate): Result<Unit> =
        engine.connect(device.host, device.id)

    override fun disconnect() = engine.close()

    override fun key(key: RemoteKey): Result<Unit> {
        val code = when (key) {
            RemoteKey.POWER -> 26
            RemoteKey.HOME -> 3
            RemoteKey.BACK -> 4
            RemoteKey.UP -> 19
            RemoteKey.DOWN -> 20
            RemoteKey.LEFT -> 21
            RemoteKey.RIGHT -> 22
            RemoteKey.OK -> 23
            RemoteKey.VOLUME_UP -> 24
            RemoteKey.VOLUME_DOWN -> 25
            RemoteKey.MUTE -> 91
            RemoteKey.PLAY_PAUSE -> 85
            RemoteKey.STOP -> 86
            RemoteKey.PREVIOUS -> 88
            RemoteKey.NEXT -> 87
            RemoteKey.REWIND -> 89
            RemoteKey.FAST_FORWARD -> 90
            RemoteKey.GUIDE -> 172
            RemoteKey.INPUT -> 178
            RemoteKey.SETTINGS -> 176
        }
        return engine.sendKey(code)
    }

    override fun text(value: String): Result<Unit> = engine.sendText(value)
    override fun volume(delta: Int): Result<Unit> = engine.volumeDelta(delta)
}
