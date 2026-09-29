package com.haddadian.universalremote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import com.haddadian.universalremote.connection.protocol.ProtocolIds

enum class AuthState { NOT_REQUIRED, REQUIRED, WAITING_FOR_USER, CREDENTIAL_ENTERED, AUTHENTICATED, FAILED }

data class AuthSession(
    val deviceId: String,
    val protocol: String,
    val state: AuthState,
    val message: String
)

/** Vendor authentication is explicit; no generic PIN is treated as a universal credential. */
interface AuthAdapter {
    suspend fun begin(device: TvDevice): AuthSession
    suspend fun submitCode(device: TvDevice, code: String): AuthSession
}

class NoAuthAdapter(private val message: String) : AuthAdapter {
    override suspend fun begin(device: TvDevice) = AuthSession(device.id, device.protocol, AuthState.NOT_REQUIRED, message)
    override suspend fun submitCode(device: TvDevice, code: String) = begin(device)
}

class AndroidTvAuthAdapter(context: Context) : AuthAdapter {
    private val store = AtvCertificateStore(context.applicationContext)
    private val pairingSessions = ConcurrentHashMap<String, AndroidTvPairingEngine>()

    override suspend fun begin(device: TvDevice): AuthSession = withContext(Dispatchers.IO) {
        if (store.loadServerCertificate(device.id) != null) {
            AuthSession(device.id, device.protocol, AuthState.AUTHENTICATED, "Android TV pairing موجود است.")
        } else {
            val host = device.address
            if (host.isNullOrBlank()) {
                AuthSession(device.id, device.protocol, AuthState.FAILED, "آدرس Android TV پیدا نشد.")
            } else {
                val pairing = AndroidTvPairingEngine(store)
                pairing.start(host, device.id).fold(
                    onSuccess = {
                        pairingSessions.put(device.id, pairing)?.close()
                        AuthSession(device.id, device.protocol, AuthState.WAITING_FOR_USER, "کد ۶ رقمی نمایش‌داده‌شده روی Android TV را وارد کنید.")
                    },
                    onFailure = {
                        pairing.close()
                        AuthSession(device.id, device.protocol, AuthState.FAILED, it.message ?: "شروع Pairing ناموفق بود.")
                    }
                )
            }
        }
    }

    override suspend fun submitCode(device: TvDevice, code: String): AuthSession = withContext(Dispatchers.IO) {
        if (!code.matches(Regex("(?i)[0-9a-f]{6}"))) {
            AuthSession(device.id, device.protocol, AuthState.FAILED, "کد Android TV باید دقیقاً ۶ رقم/حرف هگزادسیمال باشد.")
        } else {
            val pairing = pairingSessions[device.id]
                ?: return@withContext AuthSession(device.id, device.protocol, AuthState.FAILED, "جلسه Pairing این دستگاه منقضی شده است؛ دوباره تلاش کنید.")
            pairing.finish(code).fold(
                onSuccess = {
                    pairingSessions.remove(device.id, pairing)
                    AuthSession(device.id, device.protocol, AuthState.AUTHENTICATED, "Android TV با موفقیت Pair شد.")
                },
                onFailure = {
                    pairingSessions.remove(device.id, pairing)
                    pairing.close()
                    AuthSession(device.id, device.protocol, AuthState.FAILED, it.message ?: "Pairing رد شد.")
                }
            )
        }
    }
}

class PskAuthAdapter(private val credentialStore: com.haddadian.universalremote.security.DeviceCredentialStore) : AuthAdapter {
    override suspend fun begin(device: TvDevice): AuthSession {
        val stored = ProtocolIds.credentialAliases(device.protocol).asSequence()
            .mapNotNull { credentialStore.get(device.id, it) }
            .firstOrNull()
        return if (!stored.isNullOrBlank()) {
            AuthSession(device.id, device.protocol, AuthState.CREDENTIAL_ENTERED, "PSK ذخیره‌شده آماده استفاده است؛ اعتبار آن هنگام اتصال به تلویزیون بررسی می‌شود.")
        } else {
            AuthSession(device.id, device.protocol, AuthState.WAITING_FOR_USER, "کلید Pre-Shared Key تلویزیون را وارد کنید.")
        }
    }

    override suspend fun submitCode(device: TvDevice, code: String) =
        if (code.trim().isNotEmpty() && code.length <= 128)
            AuthSession(device.id, device.protocol, AuthState.CREDENTIAL_ENTERED, "PSK دریافت شد؛ اعتبار آن هنگام اتصال به تلویزیون بررسی می‌شود.")
        else AuthSession(device.id, device.protocol, AuthState.FAILED, "PSK معتبر نیست.")
}

/** Samsung handles authorization in its WebSocket protocol; no fake numeric PIN is requested. */
class AuthManager(context: Context) {
    private val adapters: Map<String, AuthAdapter> = mapOf(
        ProtocolIds.ANDROID_TV_REMOTE_V2 to AndroidTvAuthAdapter(context),
        ProtocolIds.SONY_IRCC to PskAuthAdapter(com.haddadian.universalremote.security.DeviceCredentialStore(context)),
        ProtocolIds.ROKU_ECP to NoAuthAdapter("Roku ECP معمولاً به احراز هویت جداگانه نیاز ندارد."),
        ProtocolIds.PHILIPS_JOINTSPACE to NoAuthAdapter("Philips JointSpace در این مسیر بدون Pairing استفاده می‌شود."),
        ProtocolIds.SAMSUNG_WS to NoAuthAdapter("Samsung WebSocket در مرحله اتصال مجوز را از خود تلویزیون دریافت می‌کند."),
        ProtocolIds.LG_SSAP to NoAuthAdapter("LG webOS در زمان register پاسخ client-key را مدیریت می‌کند.")
    )

    suspend fun begin(device: TvDevice): AuthSession =
        adapters[device.protocol]?.begin(device)
            ?: AuthSession(device.id, device.protocol, AuthState.NOT_REQUIRED, "احراز هویت اختصاصی برای این پروتکل تعریف نشده است.")

    suspend fun submitCode(device: TvDevice, code: String): AuthSession =
        adapters[device.protocol]?.submitCode(device, code)
            ?: AuthSession(device.id, device.protocol, AuthState.FAILED, "Adapter احراز هویت این پروتکل پیاده‌سازی نشده است.")
}
