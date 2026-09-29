package com.haddadian.universalremote.connection

data class FingerprintEvidence(val source: String, val value: String, val weight: Int)
data class FingerprintResult(val fingerprint: TvFingerprint, val confidence: Int, val evidence: List<FingerprintEvidence>)

class FingerprintEngine {
    fun enrich(device: DiscoveredTv): FingerprintResult {
        val f = device.fingerprint
        val evidence = buildList {
            if (!f.manufacturer.isNullOrBlank()) add(FingerprintEvidence("manufacturer", f.manufacturer, 30))
            if (!f.model.isNullOrBlank()) add(FingerprintEvidence("model", f.model, 35))
            if (!f.platform.isNullOrBlank()) add(FingerprintEvidence("platform", f.platform, 20))
            if (f.address != null) add(FingerprintEvidence("address", f.address, 5))
            device.services.take(5).forEach { add(FingerprintEvidence("service", it, 5)) }
        }
        val confidence = (device.confidence * 100).toInt().coerceAtLeast(evidence.sumOf { it.weight }.coerceAtMost(100))
        return FingerprintResult(f, confidence.coerceIn(0,100), evidence)
    }
}
