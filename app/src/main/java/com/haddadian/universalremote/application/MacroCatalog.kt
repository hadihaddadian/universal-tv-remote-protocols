package com.haddadian.universalremote.application

import com.haddadian.universalremote.Command
import com.haddadian.universalremote.domain.MacroStep
import com.haddadian.universalremote.domain.RemoteMacro

/** Curated macros. They are deterministic and contain only local remote commands. */
object MacroCatalog {
    val presets: List<RemoteMacro> = listOf(
        RemoteMacro("movie", "Movie Mode", listOf(
            MacroStep(Command.Power), MacroStep(Command.Input, 250), MacroStep(Command.VolumeDown, 120)
        )),
        RemoteMacro("quiet", "Quiet Mode", listOf(
            MacroStep(Command.Mute)
        )),
        RemoteMacro("home", "Home", listOf(
            MacroStep(Command.Home)
        ))
    )
}
