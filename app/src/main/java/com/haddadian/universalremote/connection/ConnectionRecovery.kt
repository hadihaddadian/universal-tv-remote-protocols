package com.haddadian.universalremote.connection

data class RecoveryAction(
    val title: String,
    val reason: String,
    val retry: Boolean = false
)

class ConnectionRecovery {
    fun suggest(state: ConnectionState): List<RecoveryAction> {
        val last = state.attempts.lastOrNull()
        return when {
            last == null -> listOf(
                RecoveryAction("جستجوی دوباره", "تلویزیون هنوز شناسایی نشده است.", true)
            )
            last.stage == ConnectionStage.PAIR -> listOf(
                RecoveryAction("نمایش مراحل Pairing", "احراز هویت تلویزیون کامل نشده است."),
                RecoveryAction("تلاش با روش دیگر", "در صورت وجود Adapter جایگزین.", true)
            )
            last.stage == ConnectionStage.PROBE -> listOf(
                RecoveryAction("بررسی شبکه", "سرویس اتصال شناسایی نشد."),
                RecoveryAction("جستجوی مجدد", "ممکن است سرویس تازه در شبکه ظاهر شده باشد.", true)
            )
            else -> listOf(
                RecoveryAction("تلاش دوباره", "اتصال قبلی ناموفق بود.", true),
                RecoveryAction("تغییر روش اتصال", "روش دیگری از Adapterهای ثبت‌شده امتحان شود.", true)
            )
        }
    }
}
