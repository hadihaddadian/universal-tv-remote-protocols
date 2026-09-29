package com.haddadian.universalremote.ui

import com.haddadian.universalremote.domain.Capability

/** UI projection of the canonical domain capability set. */
data class RemoteUiCapabilities(
    val navigation: Boolean,
    val pointer: Boolean,
    val keyboard: Boolean,
    val voice: Boolean,
    val appLaunch: Boolean,
    val clipboard: Boolean,
    val playback: Boolean,
    val channel: Boolean,
    val volume: Boolean,
    val power: Boolean,
    val mute: Boolean,
    val input: Boolean,
    val numberPad: Boolean
) {
    companion object {
        fun fromCanonical(capabilities: Set<Capability>): RemoteUiCapabilities = RemoteUiCapabilities(
            navigation = Capability.NAVIGATION in capabilities,
            pointer = Capability.POINTER in capabilities,
            keyboard = Capability.KEYBOARD in capabilities,
            voice = Capability.VOICE in capabilities,
            appLaunch = Capability.APP_LAUNCH in capabilities,
            clipboard = Capability.CLIPBOARD in capabilities,
            playback = Capability.PLAYBACK in capabilities,
            channel = Capability.CHANNEL in capabilities,
            volume = Capability.VOLUME in capabilities,
            power = Capability.POWER in capabilities,
            mute = Capability.MUTE in capabilities,
            input = Capability.INPUT in capabilities,
            numberPad = Capability.NUMBER_PAD in capabilities
        )
    }
}
