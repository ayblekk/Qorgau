package kz.qorgau.scamguardian.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `monitor toggles gate each source independently`() {
        val settings = AppSettings(
            monitorSms = true,
            monitorWhatsapp = false,
            monitorTelegram = true,
        )
        assertTrue(settings.isMonitoringEnabled(SourceApp.SMS))
        assertFalse(settings.isMonitoringEnabled(SourceApp.WHATSAPP))
        assertTrue(settings.isMonitoringEnabled(SourceApp.TELEGRAM))
        assertTrue(settings.isMonitoringEnabled(SourceApp.MANUAL))
    }

    @Test
    fun `other is monitored only if at least one channel is on`() {
        assertFalse(
            AppSettings(
                monitorSms = false,
                monitorWhatsapp = false,
                monitorTelegram = false,
            ).isMonitoringEnabled(SourceApp.OTHER),
        )
        assertTrue(
            AppSettings(monitorSms = true).isMonitoringEnabled(SourceApp.OTHER),
        )
    }
}
