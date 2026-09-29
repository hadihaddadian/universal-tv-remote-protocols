package com.haddadian.universalremote.application

import com.haddadian.universalremote.domain.ConnectionPhase

data class ConnectionAdvice(val title: String, val detail: String, val retry: Boolean = false)

/**
 * v29 fix: previously referenced a duplicate `domain.ConnectionState`/
 * `domain.ConnectionPhase` enum pair that collided with (and has been
 * removed in favor of) the sealed-interface ConnectionPhase already used by
 * RemoteViewModel in RemoteModels.kt. Rewritten against the real type so it
 * can be wired into the UI layer directly.
 */
class ConnectionAssistant {
    fun advise(phase: ConnectionPhase, lastError: String? = null): ConnectionAdvice = when (phase) {
        is ConnectionPhase.Pairing ->
            ConnectionAdvice("احراز هویت لازم است", phase.message)
        is ConnectionPhase.Failed ->
            ConnectionAdvice("اتصال برقرار نشد", lastError ?: phase.message, retry = phase.canRetry)
        is ConnectionPhase.Ready ->
            ConnectionAdvice("اتصال آماده است", "ریموت قابل استفاده است.")
        is ConnectionPhase.Connecting ->
            ConnectionAdvice("در حال اتصال", phase.label)
        is ConnectionPhase.Discovering ->
            ConnectionAdvice("در حال جستجو", "به دنبال تلویزیون‌های موجود در شبکه هستیم.")
        is ConnectionPhase.Selecting ->
            ConnectionAdvice("انتخاب روش اتصال", "روش اتصال: ${phase.method.title}")
        is ConnectionPhase.Idle ->
            ConnectionAdvice("آماده به کار", "برای شروع، جستجو را بزنید.")
    }
}
