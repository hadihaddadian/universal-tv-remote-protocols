package com.haddadian.universalremote.ai

/**
 * Final safety boundary for commands produced by the AI layer.
 * The AI is never allowed to execute arbitrary application actions.
 */
class AiCommandBoundary {
    fun validate(command: AiCommand): Result<AiCommand> = runCatching {
        val rawName = command.name.trim().uppercase()
        require(rawName in AiCommandNames.allowed) { "Command is outside the TV-control allow-list" }
        val name = if (rawName == "ENTER") "OK" else rawName
        require(command.confidence in 0f..1f) { "Invalid confidence" }

        when (name) {
            "SET_VOLUME" -> require(command.argument?.toIntOrNull() in 0..100) {
                "Volume must be between 0 and 100"
            }
            "OPEN_APP" -> require(
                !command.argument.isNullOrBlank() &&
                    command.argument.length <= 128 &&
                    command.argument.all { it.isLetterOrDigit() || it in " ._-" }
            ) { "Invalid app identifier" }
        }

        command.copy(name = name)
    }
}
