package com.haddadian.universalremote.ai

/** Provider secrets are never accepted or stored by the Android client. */
data class AiConfiguration(
    val enabled: Boolean = false,
    val backendUrl: String = ""
) {
    fun isValid(): Boolean =
        enabled && backendUrl.startsWith("https://", ignoreCase = true)
}
