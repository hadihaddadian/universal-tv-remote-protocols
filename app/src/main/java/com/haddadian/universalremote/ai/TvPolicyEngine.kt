package com.haddadian.universalremote.ai

class TvPolicyEngine {
    fun authorize(intent: TvIntent?, context: TvContext): Boolean {
        if (intent == null || context.connectedDeviceCount <= 0) return false

        return when (intent) {
            TvIntent.Power -> true
            is TvIntent.SetVolume -> intent.level in 0..100
            is TvIntent.AdjustVolume -> intent.amount in -20..20
            is TvIntent.OpenApp -> intent.app.length in 1..128
            else -> true
        }
    }
}
