package com.haddadian.universalremote.ai

class TvIntentValidator {
    fun validate(x: AiInterpretation, context: TvContext): AiInterpretation {
        val original = x.intent ?: return x

        if (x.confidence < 0.72f) {
            return x.copy(
                intent = null,
                clarification = "منظور فرمان را دقیق متوجه نشدم."
            )
        }

        val sanitized: TvIntent? = when (original) {
            is TvIntent.SetVolume ->
                original.copy(level = original.level.coerceIn(0, 100))
            is TvIntent.AdjustVolume ->
                original.copy(amount = original.amount.coerceIn(-20, 20))
            is TvIntent.OpenApp ->
                original.takeIf { it.app.isNotBlank() && it.app.length <= 128 }
            else -> original
        }

        if (sanitized == null) {
            return x.copy(
                intent = null,
                clarification = "پارامتر فرمان معتبر نیست."
            )
        }

        return x.copy(
            intent = sanitized,
            needsConfirmation = sanitized is TvIntent.Power && context.powered == true
        )
    }
}
