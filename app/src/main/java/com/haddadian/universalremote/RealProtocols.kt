package com.haddadian.universalremote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import com.haddadian.universalremote.security.LocalTvHttpClient
import com.haddadian.universalremote.security.DeviceCredentialStore
import com.haddadian.universalremote.security.DeviceCertificatePinStore
import com.haddadian.universalremote.security.LocalTvTlsClient
import android.content.Context
import com.haddadian.universalremote.reliability.ConnectionHealth
import com.haddadian.universalremote.infra.InfraredController

/*
 * Real protocol adapters for major TV platforms.
 *
 * These adapters deliberately use documented/known local protocols and do not
 * bypass authentication. Some consumer-TV protocols vary by model/year.
 */

interface TvTransport {
    suspend fun startVoice(scope: kotlinx.coroutines.CoroutineScope): Result<Unit> =
        Result.failure(UnsupportedOperationException("Voice input is not supported by this transport"))
    suspend fun stopVoice() {}
    suspend fun sendText(device: TvDevice, text: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Text input is not supported by this transport"))
    suspend fun launchApp(device: TvDevice, appLink: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("App launch is not supported by this transport"))
    suspend fun connect(device: TvDevice, auth: Credentials): Result<Unit>
    suspend fun send(device: TvDevice, command: Command): Result<Unit>
    suspend fun healthCheck(device: TvDevice): ConnectionHealth = ConnectionHealth.UNKNOWN
    suspend fun disconnect(device: TvDevice) { close() }
    fun close() {}
}

data class Credentials(
    val pin: String? = null,
    val token: String? = null,
    val psk: String? = null
)

private fun key(command: Command): String? = when(command) {
    Command.Power -> "POWER"
    Command.Home -> "HOME"
    Command.Back -> "BACK"
    Command.Up -> "UP"
    Command.Down -> "DOWN"
    Command.Left -> "LEFT"
    Command.Right -> "RIGHT"
    Command.Ok -> "ENTER"
    Command.VolumeUp -> "VOLUP"
    Command.VolumeDown -> "VOLDOWN"
    Command.Mute -> "MUTE"
    Command.PlayPause -> "PLAYPAUSE"
    Command.Stop -> "STOP"
    Command.Previous -> "PREV"
    Command.Next -> "NEXT"
    Command.Rewind -> "REWIND"
    Command.FastForward -> "FF"
    Command.Guide -> "GUIDE"
    Command.Input -> "SOURCE"
    is Command.Number -> if (command.value in 0..9) command.value.toString() else null
    is Command.SendText, is Command.LaunchApp -> null
}

/** Samsung Smart TV WebSocket protocol (ports 8001/8002, token on pairing). */
class SamsungTransport(
    private val client: OkHttpClient? = null,
    private val credentialStore: DeviceCredentialStore? = null,
    private val pinStore: DeviceCertificatePinStore? = null
) : TvTransport {
    private var ws: WebSocket? = null
    private var token: String? = null

    override suspend fun connect(device: TvDevice, auth: Credentials): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val host = device.address ?: return@suspendCancellableCoroutine cont.resume(
                Result.failure(IllegalArgumentException("Samsung TV IP missing"))
            )
            val name = Base64.getEncoder().encodeToString("Universal TV Remote".toByteArray())
            val suppliedToken = auth.token?.takeIf { it.isNotBlank() } ?: credentialStore?.get(device.id, "samsung-ws")
            token = suppliedToken
            val tok = suppliedToken?.let { "&token=${URLEncoder.encode(it, "UTF-8")}" } ?: ""
            val url = "wss://$host:8002/api/v2/channels/samsung.remote.control?name=$name$tok"
            val activeClient = client ?: if (pinStore != null) LocalTvTlsClient(pinStore).create(host, device.id) else LocalTvHttpClient.create()
            val request = Request.Builder().url(url).build()
            var completed = false
            fun finish(result: Result<Unit>) {
                if (!completed) { completed = true; cont.resume(result) }
            }
            ws = activeClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val certificate = response.handshake?.peerCertificates?.firstOrNull()
                    if (certificate != null && pinStore != null) {
                        pinStore.put(device.id, CertificatePinner.pin(certificate))
                    }
                    finish(Result.success(Unit))
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val root = JSONObject(text)
                        val data = root.optJSONObject("data")
                        val received = data?.optString("token")?.takeIf { it.isNotBlank() }
                        if (received != null) {
                            token = received
                            credentialStore?.put(device.id, "samsung-ws", received)
                        }
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    ws = null
                    finish(Result.failure(t))
                }
            })
            cont.invokeOnCancellation { ws?.cancel() }
        }

    override suspend fun send(device: TvDevice, command: Command): Result<Unit> =
        withContext(Dispatchers.IO) {
            val k = key(command) ?: return@withContext Result.success(Unit)
            val msg = JSONObject()
                .put("method", "ms.remote.control")
                .put("params", JSONObject()
                    .put("Cmd", "Click")
                    .put("DataOfCmd", "KEY_$k")
                    .put("Option", "false")
                    .put("TypeOfRemote", "SendRemoteKey"))
            if (ws?.send(msg.toString()) == true) Result.success(Unit)
            else Result.failure(IllegalStateException("Samsung WebSocket is not connected"))
        }

    override suspend fun healthCheck(device: TvDevice): ConnectionHealth =
        if (ws != null) ConnectionHealth.HEALTHY else ConnectionHealth.UNHEALTHY

    override suspend fun disconnect(device: TvDevice) { close() }
    override fun close() {
        ws?.close(1000, "disconnect")
        ws = null
        token = null
    }
}

/** LG webOS SSAP WebSocket protocol, including pairing registration. */
class LgWebOsTransport(
    private val client: OkHttpClient = LocalTvHttpClient.create(),
    private val credentialStore: DeviceCredentialStore? = null
) : TvTransport {
    private var ws: WebSocket? = null
    private var requestId = 0
    private var muted = false

    override suspend fun connect(device: TvDevice, auth: Credentials): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val host = device.address ?: return@suspendCancellableCoroutine cont.resume(
                Result.failure(IllegalArgumentException("LG TV IP missing"))
            )
            val storedClientKey = auth.token?.takeIf { it.isNotBlank() }
                ?: credentialStore?.get(device.id, "lg-ssap")
            var completed = false
            fun finish(result: Result<Unit>) {
                if (!completed) {
                    completed = true
                    cont.resume(result)
                }
            }
            val req = Request.Builder().url("ws://$host:3000").build()
            ws = client.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val manifest = JSONObject()
                        .put("manifestVersion", 1)
                        .put("appVersion", "1.0")
                        .put("signed", JSONObject().put("created", "2026"))
                        .put("permissions", JSONArray(listOf(
                            "LAUNCH", "LAUNCH_WEBAPP", "CONTROL_AUDIO", "CONTROL_POWER",
                            "READ_CURRENT_CHANNEL", "READ_RUNNING_APPS", "CONTROL_INPUT_TEXT",
                            "CONTROL_MOUSE_AND_KEYBOARD"
                        )))
                    val payload = JSONObject()
                        .put("type", "register")
                        .put("id", "register_1")
                        .put("payload", JSONObject()
                            .put("manifest", manifest)
                            .put("pairingType", "PROMPT")
                            .apply { if (storedClientKey != null) put("client-key", storedClientKey) })
                    if (!webSocket.send(payload.toString())) finish(Result.failure(IllegalStateException("LG registration request was rejected")))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val message = JSONObject(text)
                        if (message.optString("id") != "register_1") return
                        val payload = message.optJSONObject("payload")
                        val clientKey = payload?.optString("client-key")?.takeIf { it.isNotBlank() }
                        if (clientKey != null) credentialStore?.put(device.id, "lg-ssap", clientKey)
                        val error = payload?.optString("error")
                        if (!error.isNullOrBlank()) finish(Result.failure(IllegalStateException("LG registration failed: $error")))
                        else finish(Result.success(Unit))
                    }.onFailure { finish(Result.failure(it)) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    ws = null
                    finish(Result.failure(t))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    ws = null
                    if (!completed) finish(Result.failure(IllegalStateException("LG WebSocket closed before registration completed")))
                }
            })
            cont.invokeOnCancellation { ws?.cancel() }
        }

    override suspend fun send(device: TvDevice, command: Command): Result<Unit> =
        withContext(Dispatchers.IO) {
            val uri = when (command) {
                Command.VolumeUp -> "ssap://audio/volumeUp"
                Command.VolumeDown -> "ssap://audio/volumeDown"
                Command.Mute -> "ssap://audio/setMute"
                Command.Power -> "ssap://system/turnOff"
                Command.PlayPause, Command.Stop, Command.Previous, Command.Next, Command.Rewind, Command.FastForward, Command.Guide, Command.Input, is Command.Number, Command.Home, Command.Back, Command.Up, Command.Down, Command.Left, Command.Right, Command.Ok -> return@withContext Result.failure(UnsupportedOperationException(
                    "LG transport does not support this command"
                ))
                is Command.SendText, is Command.LaunchApp -> return@withContext Result.failure(UnsupportedOperationException(
                    "LG transport does not support this command"
                ))
            }
            val payload = JSONObject()
                .put("type", "request")
                .put("id", "command_${++requestId}")
                .put("uri", uri)
            if (command == Command.Mute) {
                muted = !muted
                payload.put("payload", JSONObject().put("mute", muted))
            }
            if (ws?.send(payload.toString()) == true) Result.success(Unit)
            else Result.failure(IllegalStateException("LG WebSocket not connected"))
        }

    override suspend fun healthCheck(device: TvDevice): ConnectionHealth =
        if (ws?.send(JSONObject().put("type", "request").put("id", "health_${System.currentTimeMillis()}").put("uri", "ssap://system/getSystemInfo").toString()) == true) ConnectionHealth.HEALTHY
        else ConnectionHealth.UNHEALTHY

    override suspend fun disconnect(device: TvDevice) { close() }
    override fun close() {
        ws?.close(1000, "disconnect")
        ws = null
    }
}

/** Sony BRAVIA IRCC-IP: HTTP SOAP + X-Auth-PSK. */
class SonyBraviaTransport(
    private val client: OkHttpClient = LocalTvHttpClient.create()
) : TvTransport {
    private val ircc = mapOf(
        Command.Power to "AAAAAQAAAAEAAAAVAw==",
        Command.Up to "AAAAAQAAAAEAAAB0Aw==",
        Command.Down to "AAAAAQAAAAEAAAB1Aw==",
        Command.Left to "AAAAAQAAAAEAAAA0Aw==",
        Command.Right to "AAAAAQAAAAEAAAAzAw==",
        Command.Ok to "AAAAAQAAAAEAAABlAw==",
        Command.Back to "AAAAAgAAAJcAAAAjAw==",
        Command.Home to "AAAAAQAAAAEAAABgAw==",
        Command.VolumeUp to "AAAAAQAAAAEAAAASAw==",
        Command.VolumeDown to "AAAAAQAAAAEAAAATAw==",
        Command.Mute to "AAAAAQAAAAEAAAAUAw=="
    )

    // v29 fix: send(device, command) previously always failed because the PSK
    // supplied to connect() was discarded instead of being retained for later
    // calls, even though connect() already validated it. It is now cached
    // per-instance (keyed by device id) after a successful connect, so the
    // TvTransport interface contract (connect once, then send many times)
    // actually works instead of unconditionally returning failure.
    private val pskByDeviceId = mutableMapOf<String, String>()

    override suspend fun connect(device: TvDevice, auth: Credentials): Result<Unit> =
        withContext(Dispatchers.IO) {
            val psk = auth.psk
            if (device.address.isNullOrBlank())
                Result.failure(IllegalArgumentException("Sony IP missing"))
            else if (psk.isNullOrBlank())
                Result.failure(IllegalArgumentException("Sony Pre-Shared Key required"))
            else {
                pskByDeviceId[device.id] = psk
                Result.success(Unit)
            }
        }

    override suspend fun send(device: TvDevice, command: Command): Result<Unit> {
        val psk = pskByDeviceId[device.id]
            ?: return Result.failure(IllegalStateException("Sony TV is not connected; call connect() first"))
        return sendWithPsk(device, command, psk)
    }

    override suspend fun healthCheck(device: TvDevice): ConnectionHealth =
        if (pskByDeviceId.containsKey(device.id)) {
            runCatching {
                val ip = requireNotNull(device.address)
                val request = Request.Builder().url("http://$ip/sony/system").get()
                    .addHeader("X-Auth-PSK", pskByDeviceId.getValue(device.id)).build()
                client.newCall(request).execute().use { if (it.isSuccessful) ConnectionHealth.HEALTHY else ConnectionHealth.UNHEALTHY }
            }.getOrDefault(ConnectionHealth.UNHEALTHY)
        } else ConnectionHealth.UNHEALTHY

    override suspend fun disconnect(device: TvDevice) { pskByDeviceId.remove(device.id) }
    override fun close() { pskByDeviceId.clear() }

    suspend fun sendWithPsk(device: TvDevice, command: Command, psk: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val ip = device.address ?: return@withContext Result.failure(Exception("IP missing"))
            val code = ircc[command] ?: return@withContext Result.failure(Exception("Command unsupported"))
            val xml = """<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body><u:X_SendIRCC xmlns:u="urn:schemas-sony-com:service:IRCC:1"><IRCCCode>$code</IRCCCode></u:X_SendIRCC></s:Body></s:Envelope>"""
            val body = xml.toRequestBody("text/xml; charset=utf-8".toMediaType())
            val request = Request.Builder().url("http://$ip/sony/IRCC")
                .addHeader("Content-Type", "text/xml")
                .addHeader("SOAPACTION", "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"")
                .addHeader("X-Auth-PSK", psk)
                .post(body).build()
            client.newCall(request).execute().use {
                if (it.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Sony HTTP ${it.code}"))
            }
        }
}

/** Philips JointSpace v1-style HTTP protocol (older compatible sets). */
class PhilipsJointSpaceTransport(
    private val client: OkHttpClient = LocalTvHttpClient.create()
) : TvTransport {
    override suspend fun connect(device: TvDevice, auth: Credentials): Result<Unit> =
        withContext(Dispatchers.IO) {
            val ip = device.address ?: return@withContext Result.failure(Exception("IP missing"))
            val req = Request.Builder().url("http://$ip:1925/1/system").get().build()
            runCatching {
                client.newCall(req).execute().use {
                    if (!it.isSuccessful) error("Philips HTTP ${it.code}")
                }
            }.fold({Result.success(Unit)},{Result.failure(it)})
        }

    override suspend fun send(device: TvDevice, command: Command): Result<Unit> =
        withContext(Dispatchers.IO) {
            val ip = device.address ?: return@withContext Result.failure(Exception("IP missing"))
            val k = when(command) {
                Command.Power -> "Standby"
                Command.Home -> "Home"
                Command.Back -> "Back"
                Command.Up -> "CursorUp"
                Command.Down -> "CursorDown"
                Command.Left -> "CursorLeft"
                Command.Right -> "CursorRight"
                Command.Ok -> "Confirm"
                Command.VolumeUp -> "VolumeUp"
                Command.VolumeDown -> "VolumeDown"
                Command.Mute -> "Mute"
                Command.PlayPause -> "PlayPause"
                Command.Stop -> "Stop"
                Command.Previous -> "Previous"
                Command.Next -> "Next"
                Command.Rewind -> "Rewind"
                Command.FastForward -> "FastForward"
                Command.Guide -> "Guide"
                Command.Input -> "Source"
                is Command.Number -> if (command.value in 0..9) "Digit${command.value}" else return@withContext Result.failure(IllegalArgumentException("Channel digit must be 0..9"))
                is Command.SendText, is Command.LaunchApp -> return@withContext Result.failure(UnsupportedOperationException("Philips command is not supported"))
            }
            val body = JSONObject().put("key", k).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("http://$ip:1925/1/input/key")
                .post(body).build()
            client.newCall(req).execute().use {
                if (it.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Philips HTTP ${it.code}"))
            }
        }

    override suspend fun healthCheck(device: TvDevice): ConnectionHealth =
        runCatching {
            val ip = requireNotNull(device.address)
            client.newCall(Request.Builder().url("http://$ip:1925/1/system").get().build()).execute().use {
                if (it.isSuccessful) ConnectionHealth.HEALTHY else ConnectionHealth.UNHEALTHY
            }
        }.getOrDefault(ConnectionHealth.UNHEALTHY)
}

/** Generic built-in IR transport. The profile is deliberately model-agnostic and uses the explicit NEC fallback table in InfraredController. */
class GenericIrTvTransport(private val controller: InfraredController) : TvTransport {
    override suspend fun connect(device: TvDevice, auth: Credentials): Result<Unit> =
        if (controller.isAvailable()) Result.success(Unit)
        else Result.failure(IllegalStateException("IR emitter unavailable"))

    override suspend fun send(device: TvDevice, command: Command): Result<Unit> = controller.send(command)

    override suspend fun healthCheck(device: TvDevice): ConnectionHealth =
        if (controller.isAvailable()) ConnectionHealth.HEALTHY else ConnectionHealth.UNHEALTHY
}

/** Roku ECP, local REST API + SSDP discovery. */
class RokuEcpTransport(
    private val client: OkHttpClient = LocalTvHttpClient.create()
) : TvTransport {
    override suspend fun connect(device: TvDevice, auth: Credentials) =
        withContext(Dispatchers.IO) {
            val ip=device.address ?: return@withContext Result.failure(Exception("IP missing"))
            val req=Request.Builder().url("http://$ip:8060/query/device-info").get().build()
            runCatching { client.newCall(req).execute().use { if(!it.isSuccessful) error("Roku HTTP ${it.code}") } }
                .fold({Result.success(Unit)},{Result.failure(it)})
        }

    override suspend fun send(device: TvDevice, command: Command): Result<Unit> =
        withContext(Dispatchers.IO) {
            val ip=device.address ?: return@withContext Result.failure(Exception("IP missing"))
            val k=when(command){
                Command.Power->"PowerOff"; Command.Home->"Home"; Command.Back->"Back"
                Command.Up->"Up"; Command.Down->"Down"; Command.Left->"Left"; Command.Right->"Right"
                Command.Ok->"Select"; Command.VolumeUp->"VolumeUp"; Command.VolumeDown->"VolumeDown"
                Command.Mute->"VolumeMute"
                Command.PlayPause -> "Play"
                Command.Stop -> "Stop"
                Command.Previous -> "Rev"
                Command.Next -> "Fwd"
                Command.Rewind -> "Rev"
                Command.FastForward -> "Fwd"
                Command.Guide -> "Info"
                Command.Input -> "InputTuner"
                is Command.Number -> if (command.value in 0..9) "Lit_${command.value}" else return@withContext Result.failure(IllegalArgumentException("Roku digit must be 0..9"))
                is Command.SendText, is Command.LaunchApp -> return@withContext Result.failure(UnsupportedOperationException("Roku command is not supported"))
            }
            val req=Request.Builder().url("http://$ip:8060/keypress/$k")
                .post(ByteArray(0).toRequestBody(null)).build()
            client.newCall(req).execute().use {
                if(it.isSuccessful) Result.success(Unit) else Result.failure(Exception("Roku HTTP ${it.code}"))
            }
        }

    override suspend fun healthCheck(device: TvDevice): ConnectionHealth =
        runCatching {
            val ip = requireNotNull(device.address)
            client.newCall(Request.Builder().url("http://$ip:8060/query/device-info").get().build()).execute().use {
                if (it.isSuccessful) ConnectionHealth.HEALTHY else ConnectionHealth.UNHEALTHY
            }
        }.getOrDefault(ConnectionHealth.UNHEALTHY)
}

