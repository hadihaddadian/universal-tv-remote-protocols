package com.haddadian.universalremote.domain


/** Canonical device model shared by discovery, authentication and transports. */
data class TvDevice(
    val id: String,
    val name: String,
    val address: String?,
    val transport: String,
    val protocol: String,
    val capabilities: Set<String> = emptySet(),
    val stableIdentity: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val firmware: String? = null
)

/** UI-neutral connection methods exposed by the application. */
enum class ConnectionMethod(val title: String, val subtitle: String) {
    AUTO("هوشمند", "انتخاب خودکار بهترین مسیر"),
    WIFI("Wi‑Fi / LAN", "اتصال مستقیم در شبکه محلی"),
    BLUETOOTH("Bluetooth", "برای دستگاه‌های سازگار"),
    INFRARED("مادون قرمز", "نیازمند فرستنده IR در موبایل"),
    INTERNET("اینترنت", "برای پروتکل‌های ابری پشتیبانی‌شده"),
    EXTERNAL_MODEM("مودم خارجی", "اتصال از مسیر شبکه محلی مودم"),
    INTERNAL_MODEM("مودم داخلی", "وقتی تلویزیون از هات‌اسپات موبایل استفاده می‌کند")
}

sealed interface ConnectionPhase {
    data object Idle : ConnectionPhase
    data object Discovering : ConnectionPhase
    data class Selecting(val method: ConnectionMethod) : ConnectionPhase
    data class Connecting(val method: ConnectionMethod, val step: Int, val total: Int, val label: String) : ConnectionPhase
    data class Pairing(val method: ConnectionMethod, val message: String) : ConnectionPhase
    data class Ready(val method: ConnectionMethod) : ConnectionPhase
    data class Failed(val message: String, val canRetry: Boolean = true) : ConnectionPhase
}

