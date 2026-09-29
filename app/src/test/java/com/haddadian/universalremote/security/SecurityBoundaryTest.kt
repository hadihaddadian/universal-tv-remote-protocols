package com.haddadian.universalremote.security

import org.junit.Assert.*
import org.junit.Test

class SecurityBoundaryTest {
    @Test fun rejectsHttp() {
        val p = BackendSecurityPolicy(setOf("ai.example.com"))
        assertFalse(p.validate("http://ai.example.com"))
    }

    @Test fun rejectsUnapprovedHost() {
        val p = BackendSecurityPolicy(setOf("ai.example.com"))
        assertFalse(p.validate("https://evil.example.com"))
    }

    @Test fun cloudAiOffByDefault() {
        assertFalse(PrivacyPolicy().maySendVoiceToBackend(PrivacyPreferences()))
    }
}