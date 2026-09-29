package com.haddadian.universalremote.ai

interface AiInterpreter {
    suspend fun interpret(text: String, context: TvContext): AiInterpretation
}

class AiTvCommandEngine(
    private val online: AiInterpreter?,
    private val offline: OfflineTvCommandParser = OfflineTvCommandParser(),
    private val validator: TvIntentValidator = TvIntentValidator()
) {
    suspend fun interpret(text: String, context: TvContext): AiInterpretation {
        val interpretation = runCatching {
            online?.interpret(text, context)
        }.getOrNull() ?: offline.parse(text, context)

        return validator.validate(interpretation, context)
    }
}
