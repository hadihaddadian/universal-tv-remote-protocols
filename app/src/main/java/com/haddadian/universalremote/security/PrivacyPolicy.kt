package com.haddadian.universalremote.security

data class PrivacyPreferences(
    val cloudAiEnabled: Boolean = false,
    val voiceHistoryEnabled: Boolean = false,
    val diagnosticsUploadEnabled: Boolean = false
)

class PrivacyPolicy {
    fun maySendVoiceToBackend(p: PrivacyPreferences) = p.cloudAiEnabled
    fun mayStoreVoiceHistory(p: PrivacyPreferences) = p.voiceHistoryEnabled
    fun mayUploadDiagnostics(p: PrivacyPreferences) = p.diagnosticsUploadEnabled
}