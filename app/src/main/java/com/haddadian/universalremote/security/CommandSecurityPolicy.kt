package com.haddadian.universalremote.security

import com.haddadian.universalremote.domain.CanonicalCommand

class CommandSecurityPolicy {
    fun isAllowed(command: CanonicalCommand): Boolean = when (command) {
        is CanonicalCommand.LaunchApp -> command.app.length <= 80
        is CanonicalCommand.SetVolume -> command.level in 0..100
        is CanonicalCommand.SetChannel -> command.channel in 0..9999
        is CanonicalCommand.SetInput -> command.input.length <= 40
        else -> true
    }
}
