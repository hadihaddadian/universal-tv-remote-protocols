package com.haddadian.universalremote

import android.content.Context
import android.hardware.ConsumerIrManager

class IrFallback(context: Context) {
    private val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun available(): Boolean = ir?.hasIrEmitter() == true

    fun transmit(carrierHz: Int, patternMicros: IntArray): Result<Unit> = runCatching {
        require(available()) { "This phone has no IR emitter" }
        require(carrierHz in 20_000..60_000) { "Unsupported IR carrier" }
        ir!!.transmit(carrierHz, patternMicros)
    }
}
