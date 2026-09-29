package com.haddadian.universalremote.ai

class CommandOrchestrator(
    private val local: LocalCommandParser,
    private val validator: TvIntentValidator,
    private val policy: TvPolicyEngine,
    private val online: AiInterpreter?
) {
    suspend fun interpret(text: String, context: TvContext): AiInterpretation {
        val localResult = local.parse(text)
        if (localResult != null) {
            val checked = validator.validate(localResult, context)
            if (policy.authorize(checked.intent, context)) return checked
        }

        val aiResult = online?.let { runCatching { it.interpret(text, context) }.getOrNull() }
            ?: AiInterpretation(null, 0f, clarification = "فرمان پشتیبانی نمی‌شود.")

        val checked = validator.validate(aiResult, context)
        return if (policy.authorize(checked.intent, context)) checked
        else checked.copy(intent = null, clarification = "این فرمان در محدوده کنترل تلویزیون نیست.")
    }
}
