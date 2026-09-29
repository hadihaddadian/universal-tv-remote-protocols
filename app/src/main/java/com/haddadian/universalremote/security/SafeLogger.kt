package com.haddadian.universalremote.security

import android.util.Log

/**
 * Best-effort log boundary. Known credential fields and sensitive URL query
 * parameters are always redacted; throwable messages are sanitized separately
 * instead of passing the original Throwable (which could contain secrets).
 */
object SafeLogger {
    private val secrets = listOf(
        Regex("""(?i)\bbearer\s+[A-Za-z0-9._~+/=-]+"""),
        Regex("""(?is)(api[_-]?key|access[_-]?token|refresh[_-]?token|client[-_]?key|psk|password|secret|credential|authorization|x-auth-psk)\s*[:=]\s*[\"']?[^\s,;\]}\"']+"""),
        Regex("""(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|client[-_]?key|psk|password|secret|credential)\s*\n\s*[:=]\s*[^\s,;\]}\"']+"""),
        Regex("""(?i)([?&](?:api[_-]?key|access[_-]?token|refresh[_-]?token|client[-_]?key|psk|password|secret|credential|authorization)=)[^&#\s]+""")
    )

    fun sanitize(s: String): String = secrets.fold(s) { acc, pattern ->
        pattern.replace(acc) { match ->
            val lower = match.value.lowercase()
            when {
                lower.startsWith("bearer") -> "Bearer [REDACTED]"
                match.value.firstOrNull() == '?' || match.value.firstOrNull() == '&' ->
                    match.value.substringBefore('=') + "=[REDACTED]"
                else -> match.value.substringBefore(':', match.value.substringBefore('=')).trim() + ": [REDACTED]"
            }
        }
    }

    fun d(tag: String, msg: String) = Log.d(tag, sanitize(msg))
    fun w(tag: String, msg: String) = Log.w(tag, sanitize(msg))
    fun e(tag: String, msg: String, t: Throwable? = null) {
        val detail = t?.let { "${it::class.java.simpleName}: ${sanitize(it.message.orEmpty())}" }
        Log.e(tag, listOf(sanitize(msg), detail).filter { !it.isNullOrBlank() }.joinToString(" | "))
    }
}
