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
    val monitorInstagram: Boolean = true,
    val monitorMessenger: Boolean = true,
    val monitorViber: Boolean = true,
    val monitorVk: Boolean = true,
    val monitorOk: Boolean = true,
) {
    fun isMonitoringEnabled(source: SourceApp): Boolean =
        when (source) {
            SourceApp.SMS -> monitorSms
            SourceApp.WHATSAPP -> monitorWhatsapp
            SourceApp.TELEGRAM -> monitorTelegram
            SourceApp.INSTAGRAM -> monitorInstagram
            SourceApp.MESSENGER -> monitorMessenger
            SourceApp.VIBER -> monitorViber
            SourceApp.VK -> monitorVk
            SourceApp.OK -> monitorOk
            // Other messengers monitored when at least one first-class channel is on.
            SourceApp.OTHER -> anyChannelEnabled()
            SourceApp.MANUAL -> true
        }

    private fun anyChannelEnabled(): Boolean =
        monitorSms ||
            monitorWhatsapp ||
            monitorTelegram ||
            monitorInstagram ||
            monitorMessenger ||
            monitorViber ||
            monitorVk ||
            monitorOk
}
