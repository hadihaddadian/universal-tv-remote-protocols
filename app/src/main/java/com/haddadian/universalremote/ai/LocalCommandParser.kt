package com.haddadian.universalremote.ai

class LocalCommandParser {
    fun parse(text: String): AiInterpretation? {
        val t = text.trim().lowercase()
        return when {
            t.contains("خانه") || t.contains("هوم") || t == "home" ->
                AiInterpretation(TvIntent.Home, 0.98f)
            t.contains("برگرد") || t.contains("عقب") || t == "back" ->
                AiInterpretation(TvIntent.Back, 0.96f)
            t.contains("بی صدا") || t.contains("بی‌صدا") || t.contains("mute") ->
                AiInterpretation(TvIntent.Mute, 0.99f)
            t.contains("بلند") || t.contains("زیاد") ->
                AiInterpretation(TvIntent.AdjustVolume(3), 0.84f)
            t.contains("کم") || t.contains("پایین") ->
                AiInterpretation(TvIntent.AdjustVolume(-3), 0.84f)
            t.contains("پخش") || t.contains("play") ->
                AiInterpretation(TvIntent.Play, 0.96f)
            t.contains("توقف") || t.contains("pause") ->
                AiInterpretation(TvIntent.Pause, 0.92f)
            else -> null
        }
    }
}
