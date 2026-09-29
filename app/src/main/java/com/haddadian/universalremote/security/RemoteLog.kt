package com.haddadian.universalremote.security

/** Central, redacted diagnostics categories. Never pass credentials or pairing material. */
object RemoteLog {
    fun discovery(message: String) = SafeLogger.d("Remote/Discovery", message)
    fun connection(message: String) = SafeLogger.d("Remote/Connection", message)
    fun protocol(message: String) = SafeLogger.d("Remote/Protocol", message)
    fun command(message: String) = SafeLogger.d("Remote/Command", message)
    fun recovery(message: String) = SafeLogger.d("Remote/Recovery", message)
    fun security(message: String) = SafeLogger.w("Remote/Security", message)
}
