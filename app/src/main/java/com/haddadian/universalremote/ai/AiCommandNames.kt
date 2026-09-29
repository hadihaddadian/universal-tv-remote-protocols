package com.haddadian.universalremote.ai

/** Single source of truth for command names accepted at the AI safety boundary. */
internal object AiCommandNames {
    val allowed: Set<String> = setOf(
        "HOME", "BACK", "UP", "DOWN", "LEFT", "RIGHT", "ENTER",
        "VOLUME_UP", "VOLUME_DOWN", "MUTE", "PLAY_PAUSE", "POWER",
        "SET_VOLUME", "OPEN_APP"
    )
}
