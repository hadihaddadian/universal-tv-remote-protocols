package com.haddadian.universalremote.ai

sealed interface TvIntent {
    data object Power : TvIntent
    data object Home : TvIntent
    data object Back : TvIntent
    data object Up : TvIntent
    data object Down : TvIntent
    data object Left : TvIntent
    data object Right : TvIntent
    data object Ok : TvIntent
    data object Mute : TvIntent

    data class SetVolume(val level: Int) : TvIntent
    data class AdjustVolume(val amount: Int) : TvIntent
    data class OpenApp(val app: String) : TvIntent

    data object Play : TvIntent
    data object Pause : TvIntent
    data object Stop : TvIntent
    data object Next : TvIntent
    data object Previous : TvIntent
    data object ChannelUp : TvIntent
    data object ChannelDown : TvIntent
    data object Input : TvIntent
}

data class TvContext(
    val volume: Int? = null,
    val muted: Boolean? = null,
    val powered: Boolean? = null,
    val lastIntent: TvIntent? = null,
    val connectedDeviceCount: Int = 1
)

data class AiInterpretation(
    val intent: TvIntent? = null,
    val confidence: Float = 0f,
    val needsConfirmation: Boolean = false,
    val clarification: String? = null
)
