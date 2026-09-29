package com.haddadian.universalremote.reliability

import com.haddadian.universalremote.Command

enum class CommandDeliveryClass { ORDERED, COALESCABLE, IDEMPOTENT }

data class RemoteCommandPolicy(val delivery: CommandDeliveryClass, val minIntervalMs: Long = 0)

/** Central policy point; it never reports a dropped user command as successful. */
class CommandPolicyEngine {
    fun policy(command: Command): RemoteCommandPolicy = when (command) {
        Command.VolumeUp, Command.VolumeDown -> RemoteCommandPolicy(CommandDeliveryClass.COALESCABLE, 25)
        Command.Power -> RemoteCommandPolicy(CommandDeliveryClass.IDEMPOTENT)
        else -> RemoteCommandPolicy(CommandDeliveryClass.ORDERED)
    }
}
