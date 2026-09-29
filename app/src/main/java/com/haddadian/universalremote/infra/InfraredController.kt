package com.haddadian.universalremote.infra

import android.content.Context
import android.hardware.ConsumerIrManager
import com.haddadian.universalremote.Command

class InfraredController(context: Context) {
    private val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun isAvailable(): Boolean = manager?.hasIrEmitter() == true

    fun send(command: Command): Result<Unit> {
        val ir = manager ?: return Result.failure(IllegalStateException("IR hardware unavailable"))
        if (!ir.hasIrEmitter()) return Result.failure(IllegalStateException("IR emitter unavailable"))

        // NEC carrier timing. The command table is deliberately explicit and easy to replace
        // with a model-specific protocol profile later.
        val code = when (command) {
            Command.Power -> 0x12
            Command.VolumeUp -> 0x10
            Command.VolumeDown -> 0x11
            Command.Mute -> 0x0F
            Command.Home -> 0x47
            Command.Back -> 0x4A
            Command.Up -> 0x18
            Command.Down -> 0x52
            Command.Left -> 0x08
            Command.Right -> 0x5A
            Command.Ok -> 0x1C
            Command.PlayPause -> 0x2E
            Command.Stop -> 0x2F
            Command.Previous -> 0x30
            Command.Next -> 0x31
            Command.Rewind -> 0x32
            Command.FastForward -> 0x33
            Command.Guide -> 0x44
            Command.Input -> 0x48
            is Command.Number -> if (command.value in 0..9) command.value else return Result.failure(IllegalArgumentException("IR number must be 0..9"))
            is Command.SendText, is Command.LaunchApp -> return Result.failure(UnsupportedOperationException("IR does not support text/app launch"))
        }
        val pattern = necPattern(code)
        return runCatching { ir.transmit(38_000, pattern) }
    }

    private fun necPattern(command: Int): IntArray {
        val values = ArrayList<Int>(67)
        fun bit(one: Boolean) { values += 560; values += if (one) 1_690 else 560 }
        values += 9_000; values += 4_500
        repeat(8) { bit(false) }
        repeat(8) { bit(true) }
        repeat(8) { bit(((command shr it) and 1) == 1) }
        repeat(8) { bit((((command.inv()) shr it) and 1) == 1) }
        values += 560
        return values.toIntArray()
    }
}
