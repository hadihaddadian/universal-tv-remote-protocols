package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.TvFingerprint

/** Single source of truth for protocol compatibility scoring. */
class ProtocolSelector {
    fun select(fingerprint: TvFingerprint, profiles: Collection<ProtocolProfile>): ProtocolMatch? =
        profiles.mapNotNull { profile ->
            score(profile, fingerprint)?.let { (value, reasons) -> ProtocolMatch(profile, value, reasons) }
        }.maxWithOrNull(compareBy<ProtocolMatch> { it.score }.thenBy { it.profile.manifest.id.value })

    fun score(profile: ProtocolProfile, fingerprint: TvFingerprint): Pair<Int, List<String>>? {
        val reasons = mutableListOf<String>()
        var score = 0
        if (!fingerprint.manufacturer.isNullOrBlank() &&
            (profile.manifest.manufacturer.equals(fingerprint.manufacturer, true) ||
                profile.supportedManufacturers.any { it.equals(fingerprint.manufacturer, true) })) {
            score += 60; reasons += "manufacturer"
        }
        if (!fingerprint.platform.isNullOrBlank() && profile.manifest.platform.equals(fingerprint.platform, true)) {
            score += 50; reasons += "platform"
        }
        if (!fingerprint.model.isNullOrBlank() && profile.manifest.modelPatterns.any { pattern ->
                fingerprint.model.equals(pattern, true) || fingerprint.model.contains(pattern, true)
            }) {
            score += 100; reasons += "model"
        }
        if (fingerprint.capabilities.any { capability ->
                profile.discoveryHints.any { it.equals(capability, true) }
            }) {
            score += 20; reasons += "discovery capability"
        }
        if (score == 0 && profile.manifest.id.value == "generic-ir-tv" &&
            (fingerprint.platform.equals("ir", true) || fingerprint.capabilities.any { it.equals("ir_blaster_available", true) })) {
            score = 1
            reasons += "generic IR fallback"
        }
        return (score to reasons).takeIf { score > 0 }
    }
}
