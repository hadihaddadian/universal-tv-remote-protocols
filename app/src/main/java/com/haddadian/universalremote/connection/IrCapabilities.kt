package com.haddadian.universalremote.connection

import android.content.Context
import android.hardware.ConsumerIrManager

class IrCapability(context: Context) {
    private val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    fun available(): Boolean = ir?.hasIrEmitter() == true
    fun transmit(frequency: Int, pattern: IntArray): Boolean {
        if (!available()) return false
        ir?.transmit(frequency, pattern)
        return true
    }
}
