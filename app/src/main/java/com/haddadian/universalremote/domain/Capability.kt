package com.haddadian.universalremote.domain

enum class Capability {
    NAVIGATION, POINTER, KEYBOARD, VOICE, APP_LAUNCH, CLIPBOARD, PLAYBACK, CHANNEL, VOLUME, POWER, MUTE, INPUT, NUMBER_PAD;

    companion object {
        fun fromWire(value: String): Capability? = when (value.trim().lowercase()) {
            "navigation", "dpad" -> NAVIGATION
            "pointer", "mouse" -> POINTER
            "keyboard", "text" -> KEYBOARD
            "voice" -> VOICE
            "app_launch", "apps" -> APP_LAUNCH
            "clipboard" -> CLIPBOARD
            "playback" -> PLAYBACK
            "channel" -> CHANNEL
            "volume" -> VOLUME
            "power" -> POWER
            "mute" -> MUTE
            "input", "input_switch" -> INPUT
            "number_pad", "numeric" -> NUMBER_PAD
            else -> null
        }
    }
}

fun Set<String>.toCapabilities(): Set<Capability> = mapNotNull(Capability::fromWire).toSet()
fun Set<Capability>.toWireCapabilities(): Set<String> = map { it.name.lowercase() }.toSet()
