package kz.qorgau.scamguardian.domain.model

/**
 * Domain model for app settings (maps to AppSettings single-row table).
 */
data class AppSettings(
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val sensitivity: Sensitivity = Sensitivity.MEDIUM,
    val monitorSms: Boolean = true,
    val monitorWhatsapp: Boolean = true,
    val monitorTelegram: Boolean = true,
) {
    fun isMonitoringEnabled(source: SourceApp): Boolean =
        when (source) {
            SourceApp.SMS -> monitorSms
            SourceApp.WHATSAPP -> monitorWhatsapp
            SourceApp.TELEGRAM -> monitorTelegram
            // Other messengers always monitored when at least one channel is on.
            SourceApp.OTHER -> monitorSms || monitorWhatsapp || monitorTelegram
            SourceApp.MANUAL -> true
        }
}
