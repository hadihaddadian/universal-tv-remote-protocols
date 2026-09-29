package com.haddadian.universalremote.application

import com.haddadian.universalremote.domain.RemoteMacro
import com.haddadian.universalremote.Command
import kotlinx.coroutines.delay

interface MacroCommandExecutor {
    suspend fun execute(command: Command): Result<Unit>
}

class MacroEngine(private val executor: MacroCommandExecutor) {
    suspend fun run(macro: RemoteMacro): Result<Unit> = runCatching {
        require(macro.steps.isNotEmpty()) { "Macro is empty" }
        macro.steps.forEach { step ->
            executor.execute(step.command).getOrThrow()
            if (step.delayMs > 0) delay(step.delayMs)
        }
    }
}
