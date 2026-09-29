package com.haddadian.universalremote.ai

import org.junit.Assert.*
import org.junit.Test

class CommandPolicyTest {
    @Test fun localHomeIsRecognized() {
        val r = LocalCommandParser().parse("برو خانه")
        assertEquals(TvIntent.Home, r?.intent)
        assertTrue((r?.confidence ?: 0f) > .9f)
    }

    @Test fun policyRejectsInvalidVolume() {
        val c = TvContext(volume = 20, muted = false, powered = true)
        assertFalse(TvPolicyEngine().authorize(TvIntent.SetVolume(120), c))
    }
}
