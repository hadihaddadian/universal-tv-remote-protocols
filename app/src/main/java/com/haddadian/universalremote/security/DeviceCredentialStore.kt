package com.haddadian.universalremote.security

import android.content.Context

/** Credentials are namespaced by stable device identity and protocol identity. */
class DeviceCredentialStore(context: Context) {
    private val secrets = SecureSecretStore(context.applicationContext)

    fun put(deviceId: String, protocolId: String, value: String) {
        require(deviceId.isNotBlank() && protocolId.isNotBlank() && value.isNotBlank())
        secrets.put(key(deviceId, protocolId), value)
    }
    fun get(deviceId: String, protocolId: String): String? = secrets.get(key(deviceId, protocolId))
    fun remove(deviceId: String, protocolId: String) = secrets.remove(key(deviceId, protocolId))
    private fun key(deviceId: String, protocolId: String) = "tv.credential.$protocolId.$deviceId"
}
