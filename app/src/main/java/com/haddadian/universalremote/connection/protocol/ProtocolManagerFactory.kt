package com.haddadian.universalremote.connection.protocol

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

object ProtocolManagerFactory {
    fun create(
        context: Context,
        appVersion: Int,
        remoteBaseUrl: String? = ProtocolRepositoryConfig.baseUrl,
        trustedPublicKeyBase64: String? = ProtocolRepositoryConfig.trustedPublicKeyBase64,
        refreshScope: CoroutineScope? = null
    ): ProtocolManager {
        val store = FileProtocolStore(context.applicationContext.filesDir.resolve("protocols"))
        val signatureVerifier: ProtocolSignatureVerifier =
            trustedPublicKeyBase64
                ?.takeIf { it.isNotBlank() }
                ?.let(::EcdsaProtocolSignatureVerifier)
                ?: BuiltInProtocolTrust()

        val validator = ProtocolPackageValidator(
            appVersion = appVersion,
            signatureVerifier = signatureVerifier
        )

        val trustedKey = trustedPublicKeyBase64?.takeIf { it.isNotBlank() }
        if (remoteBaseUrl != null && trustedKey == null) {
            throw IllegalArgumentException(
                "Protocol repository URL is configured but its trusted public key is missing"
            )
        }
        val remoteRepository = if (remoteBaseUrl != null) {
            require(remoteBaseUrl.startsWith("https://", ignoreCase = true)) {
                "Protocol repository must use HTTPS"
            }
            HttpProtocolRepository(remoteBaseUrl)
        } else {
            null
        }

        return ProtocolManager(
            builtInRepository = BuiltInProtocolCatalog.repository(),
            remoteRepository = remoteRepository,
            store = store,
            validator = validator,
            refreshScope = refreshScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }

    fun createRuntime(
        context: Context,
        appVersion: Int,
        remoteBaseUrl: String? = ProtocolRepositoryConfig.baseUrl,
        trustedPublicKeyBase64: String? = ProtocolRepositoryConfig.trustedPublicKeyBase64,
        refreshScope: CoroutineScope? = null
    ): ProtocolRuntime = ProtocolRuntime(
        manager = create(context, appVersion, remoteBaseUrl, trustedPublicKeyBase64, refreshScope),
        registry = ProtocolBindingRegistry.fromBuiltIns()
    )
}
