package com.haddadian.universalremote.connection

/**
 * Brand adapters are discovery candidates. A concrete model-specific adapter
 * must be selected before pairing/control; these candidates never claim that
 * an unsupported protocol is connected.
 */
abstract class BrandProtocolAdapter(private val brand: String) : ConnectionAdapter {
    override val transport = ConnectionTransport.BRAND_PROTOCOL

    override suspend fun probe(device: TvFingerprint): Result<String> =
        if (device.manufacturer?.contains(brand, ignoreCase = true) == true) {
            Result.success("$brand protocol candidate")
        } else {
            Result.failure(IllegalStateException("$brand protocol not identified"))
        }

    override suspend fun pair(device: TvFingerprint): Result<String> =
        Result.failure<String>(IllegalStateException("Pairing requires a model-specific $brand protocol adapter"))

    override suspend fun connect(device: TvFingerprint): Result<String> =
        Result.failure<String>(IllegalStateException("Control requires a model-specific $brand protocol adapter"))

    override suspend fun disconnect() = Unit
}

class SamsungAdapter : BrandProtocolAdapter("Samsung")
class LgAdapter : BrandProtocolAdapter("LG")
class SonyAdapter : BrandProtocolAdapter("Sony")
class TclAdapter : BrandProtocolAdapter("TCL")
class HisenseAdapter : BrandProtocolAdapter("Hisense")
