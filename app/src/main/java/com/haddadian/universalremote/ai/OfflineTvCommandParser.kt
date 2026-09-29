package com.haddadian.universalremote.ai

class OfflineTvCommandParser {
    fun parse(text: String, context: TvContext): AiInterpretation {
        val normalized = text
            .lowercase()
            .replace('ي', 'ی')
            .replace('ك', 'ک')

        fun result(intent: TvIntent, confidence: Float = 0.90f) =
            AiInterpretation(intent = intent, confidence = confidence)

        return when {
            normalized.contains("خاموش") -> result(TvIntent.Power, 0.96f)
            listOf("صفحه اصلی", "خانه", "هوم", "home").any(normalized::contains) ->
                result(TvIntent.Home)
            listOf("برگرد", "برگشت", "بازگشت", "back").any(normalized::contains) ->
                result(TvIntent.Back)
            listOf("بی صدا", "بی‌صدا", "ساکت", "mute", "خفه").any(normalized::contains) ->
                result(TvIntent.Mute)
            listOf("بالا", "برو بالا").any(normalized::contains) -> result(TvIntent.Up)
            listOf("پایین", "برو پایین").any(normalized::contains) -> result(TvIntent.Down)
            listOf("چپ", "برو چپ").any(normalized::contains) -> result(TvIntent.Left)
            listOf("راست", "برو راست").any(normalized::contains) -> result(TvIntent.Right)
            listOf("اوکی", "تایید", "انتخاب").any(normalized::contains) -> result(TvIntent.Ok)
            listOf("کانال بعد", "کانال بعدی", "شبکه بعد").any(normalized::contains) ->
                result(TvIntent.ChannelUp)
            listOf("کانال قبل", "کانال قبلی", "شبکه قبل").any(normalized::contains) ->
                result(TvIntent.ChannelDown)
            listOf("پخش کن", "play").any(normalized::contains) -> result(TvIntent.Play)
            listOf("مکث", "pause").any(normalized::contains) -> result(TvIntent.Pause)
            listOf("متوقف کن", "stop").any(normalized::contains) -> result(TvIntent.Stop)
            listOf("بعدی", "بعد برو").any(normalized::contains) -> result(TvIntent.Next)
            listOf("قبلی", "قبلی رو بزن").any(normalized::contains) -> result(TvIntent.Previous)
            listOf("ورودی", "input", "منبع").any(normalized::contains) -> result(TvIntent.Input)
            listOf("زیاد", "بیشتر", "بلندتر", "volume up", "ولوم بالا").any(normalized::contains) ->
                result(TvIntent.AdjustVolume(2), 0.86f)
            listOf("کم", "کمتر", "پایین تر", "پایین‌تر", "volume down", "ولوم پایین")
                .any(normalized::contains) -> result(TvIntent.AdjustVolume(-2), 0.86f)
            else -> {
                Regex("(?:صدا|ولوم).*?(\\d{1,3})")
                    .find(normalized)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?.let { result(TvIntent.SetVolume(it.coerceIn(0, 100)), 0.94f) }
                    ?: if (
                        listOf("دوباره همون", "همونو دوباره", "تکرار").any(normalized::contains) &&
                        context.lastIntent != null
                    ) {
                        result(context.lastIntent, 0.82f)
                    } else {
                        AiInterpretation(clarification = "این فرمان را برای کنترل تلویزیون متوجه نشدم.")
                    }
            }
        }
    }
}
