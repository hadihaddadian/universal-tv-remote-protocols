package com.haddadian.universalremote.application

data class IrSignal(val carrierHz: Int, val timingsMicros: List<Int>)

interface IrReceiver { suspend fun capture(): Result<IrSignal> }
interface IrTransmitter { suspend fun transmit(signal: IrSignal): Result<Unit> }

class IrLearningEngine(
    private val receiver: IrReceiver,
    private val transmitter: IrTransmitter
) {
    suspend fun learn(): Result<IrSignal> = receiver.capture()
    suspend fun send(signal: IrSignal): Result<Unit> = transmitter.transmit(signal)
}
