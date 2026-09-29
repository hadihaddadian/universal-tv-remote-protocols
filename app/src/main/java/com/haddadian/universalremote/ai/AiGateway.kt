package com.haddadian.universalremote.ai

import com.haddadian.universalremote.security.BackendSecurityPolicy

data class AiCommand(
    val name: String,
    val argument: String? = null,
    val confidence: Float
)

interface AiGateway {
    suspend fun interpret(text: String): Result<AiCommand>
}

interface AiBackendTransport {
    suspend fun interpret(
        backendUrl: String,
        accessToken: String,
        text: String
    ): Result<AiCommand>
}

/**
 * Client-side AI gateway. Provider API keys are intentionally impossible to
 * supply here; only a short-lived backend session token is accepted.
 */
class BackendAiGateway(
    private val backendUrl: String,
    private val sessionProvider: BackendSessionProvider,
    private val transport: AiBackendTransport,
    private val securityPolicy: BackendSecurityPolicy
) : AiGateway {
    override suspend fun interpret(text: String): Result<AiCommand> {
        if (text.isBlank() || text.length > 1000) {
            return Result.failure(IllegalArgumentException("Invalid voice command"))
        }
        if (!securityPolicy.validate(backendUrl)) {
            return Result.failure(IllegalArgumentException("AI backend URL is not approved"))
        }

        val now = System.currentTimeMillis() / 1000L
        val session = sessionProvider.obtain(now).getOrElse { return Result.failure(it) }
        if (session.isExpired(now)) {
            sessionProvider.invalidate()
            return Result.failure(IllegalStateException("AI backend session expired"))
        }
        return transport.interpret(backendUrl, session.accessToken, text)
    }
}
