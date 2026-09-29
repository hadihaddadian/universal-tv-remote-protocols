package com.haddadian.universalremote.security

import android.content.Context

/**
 * v29 fix: this class used to duplicate SecureSecretStore's entire AES-GCM
 * AndroidKeyStore implementation byte-for-byte (same cipher, same key-gen
 * spec, same encode/decode logic) under a different alias/prefs file. Two
 * copies of the same crypto code means a future bug fix (e.g. a padding or
 * IV-reuse issue) has to be applied twice and is easy to miss in one place.
 * This now delegates the actual encryption to SecureSecretStore and only
 * keeps the token-expiry semantics that made this class distinct.
 */
class KeystoreCredentialStore(context: Context) {
    private val secureStore = SecureSecretStore(context.applicationContext)
    private val prefs = context.getSharedPreferences("backend_session_meta", Context.MODE_PRIVATE)

    fun putBackendToken(token: String, expiresAt: Long) {
        require(token.isNotBlank())
        secureStore.put(KEY_TOKEN, token)
        prefs.edit().putLong("expiresAt", expiresAt).apply()
    }

    fun getBackendToken(now: Long): String? {
        if (prefs.getLong("expiresAt", 0L) <= now) {
            clear()
            return null
        }
        return secureStore.get(KEY_TOKEN)
    }

    fun clear() {
        secureStore.remove(KEY_TOKEN)
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "backend.token"
    }
}
