package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.BuildConfig

/** Build-time configuration for the official protocol distribution service. */
object ProtocolRepositoryConfig {
    val baseUrl: String?
        get() = BuildConfig.PROTOCOL_REPOSITORY_URL.trim().trimEnd('/').takeIf { it.isNotBlank() }

    val trustedPublicKeyBase64: String?
        get() = BuildConfig.PROTOCOL_REPOSITORY_PUBLIC_KEY.trim().takeIf { it.isNotBlank() }
}
