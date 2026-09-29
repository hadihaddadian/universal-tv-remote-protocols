package com.haddadian.universalremote.ai

import com.haddadian.universalremote.domain.CanonicalCommand

object TvIntentMapper {
    fun toCanonical(intent: TvIntent): CanonicalCommand? = when (intent) {
        TvIntent.Power -> CanonicalCommand.Power
        TvIntent.Home -> CanonicalCommand.Home
        TvIntent.Back -> CanonicalCommand.Back
        TvIntent.Up -> CanonicalCommand.Up
        TvIntent.Down -> CanonicalCommand.Down
        TvIntent.Left -> CanonicalCommand.Left
        TvIntent.Right -> CanonicalCommand.Right
        TvIntent.Ok -> CanonicalCommand.Ok
        TvIntent.Mute -> CanonicalCommand.Mute
        TvIntent.Play -> CanonicalCommand.PlayPause
        TvIntent.Pause -> CanonicalCommand.PlayPause
        is TvIntent.SetVolume -> CanonicalCommand.SetVolume(intent.level)
        is TvIntent.AdjustVolume -> if (intent.amount > 0) CanonicalCommand.VolumeUp else CanonicalCommand.VolumeDown
        is TvIntent.OpenApp -> CanonicalCommand.LaunchApp(intent.app)
        TvIntent.ChannelUp -> CanonicalCommand.SetChannel(1)
        TvIntent.ChannelDown -> CanonicalCommand.SetChannel(1)
        TvIntent.Input -> CanonicalCommand.SetInput("last")
        TvIntent.Stop, TvIntent.Next, TvIntent.Previous -> null
    }
}
