package com.haddadian.universalremote

typealias TvDevice = com.haddadian.universalremote.domain.TvDevice

sealed interface Command {
    data object Power : Command
    data object Home : Command
    data object Back : Command
    data object Up : Command
    data object Down : Command
    data object Left : Command
    data object Right : Command
    data object Ok : Command
    data object VolumeUp : Command
    data object VolumeDown : Command
    data object Mute : Command
    data object PlayPause : Command
    data object Stop : Command
    data object Previous : Command
    data object Next : Command
    data object Rewind : Command
    data object FastForward : Command
    data object Guide : Command
    data object Input : Command
    data class Number(val value: Int) : Command
    data class SendText(val text: String) : Command
    data class LaunchApp(val appLink: String) : Command
}
