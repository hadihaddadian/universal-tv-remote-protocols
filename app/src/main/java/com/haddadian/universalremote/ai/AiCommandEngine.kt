package com.haddadian.universalremote.ai

import com.haddadian.universalremote.Command
import com.haddadian.universalremote.domain.ProfileDevice
import com.haddadian.universalremote.domain.TvCapability

interface AiIntentGateway {
    suspend fun parse(text: String): Result<Command>
}

class AiCommandEngine(private val gateway: AiIntentGateway) {
    suspend fun interpret(text: String, device: ProfileDevice): Result<Command> {
        if (text.isBlank() || text.length > 1000)
            return Result.failure(IllegalArgumentException("فرمان نامعتبر است"))
        return gateway.parse(text).fold(
            onSuccess = { AiCommandPolicy.validate(it, device) },
            onFailure = { Result.failure(it) }
        )
    }
}

object AiCommandPolicy {
    fun validate(command: Command, device: ProfileDevice): Result<Command> = runCatching {
        val capability = when (command) {
            Command.Power -> TvCapability.POWER
            Command.Up, Command.Down, Command.Left, Command.Right, Command.Ok, Command.Back, Command.Home -> TvCapability.NAVIGATION
            Command.VolumeUp, Command.VolumeDown -> TvCapability.VOLUME
            Command.Mute -> TvCapability.MUTE
            Command.PlayPause, Command.Stop, Command.Previous, Command.Next, Command.Rewind, Command.FastForward -> TvCapability.PLAYBACK
            Command.Guide -> TvCapability.NAVIGATION
            Command.Input -> TvCapability.INPUT
            is Command.Number -> TvCapability.NUMBER_PAD
            is Command.SendText -> TvCapability.KEYBOARD
            is Command.LaunchApp -> TvCapability.APP_LAUNCH
        }
        require(capability in device.capabilities) { "این قابلیت توسط تلویزیون پشتیبانی نمی‌شود" }
        command
    }
}
