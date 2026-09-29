package com.haddadian.universalremote.ai

import com.haddadian.universalremote.domain.CanonicalCommand
import com.haddadian.universalremote.security.CommandSecurityPolicy

class CanonicalAiPlanner(private val policy: CommandSecurityPolicy = CommandSecurityPolicy()) {
    fun accept(command: CanonicalCommand): Result<CanonicalCommand> =
        if (policy.isAllowed(command)) Result.success(command)
        else Result.failure(IllegalArgumentException("AI command rejected by policy"))
}
