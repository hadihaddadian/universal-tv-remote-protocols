package com.haddadian.universalremote.domain

sealed interface CanonicalCommand {
    data object Power : CanonicalCommand
    data object Home : CanonicalCommand
    data object Back : CanonicalCommand
    data object Up : CanonicalCommand
    data object Down : CanonicalCommand
    data object Left : CanonicalCommand
    data object Right : CanonicalCommand
    data object Ok : CanonicalCommand
    data object Mute : CanonicalCommand
    data object VolumeUp : CanonicalCommand
    data object VolumeDown : CanonicalCommand
    data class SetVolume(val level: Int) : CanonicalCommand
    data class SetChannel(val channel: Int) : CanonicalCommand
    data class SetInput(val input: String) : CanonicalCommand
    data class LaunchApp(val app: String) : CanonicalCommand
    data object EnablePointer : CanonicalCommand
    data object DisablePointer : CanonicalCommand
    data object PlayPause : CanonicalCommand
    data object Stop : CanonicalCommand
    data object Previous : CanonicalCommand
    data object Next : CanonicalCommand
    data object Rewind : CanonicalCommand
    data object FastForward : CanonicalCommand
    data object Guide : CanonicalCommand
    data object Input : CanonicalCommand
    data class Number(val value: Int) : CanonicalCommand
}
