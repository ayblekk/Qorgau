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
            monitorInstagram = false,
            monitorMessenger = true,
            monitorViber = false,
            monitorVk = true,
            monitorOk = false,
        )
        assertTrue(settings.isMonitoringEnabled(SourceApp.SMS))
        assertFalse(settings.isMonitoringEnabled(SourceApp.WHATSAPP))
        assertTrue(settings.isMonitoringEnabled(SourceApp.TELEGRAM))
        assertFalse(settings.isMonitoringEnabled(SourceApp.INSTAGRAM))
        assertTrue(settings.isMonitoringEnabled(SourceApp.MESSENGER))
        assertFalse(settings.isMonitoringEnabled(SourceApp.VIBER))
        assertTrue(settings.isMonitoringEnabled(SourceApp.VK))
        assertFalse(settings.isMonitoringEnabled(SourceApp.OK))
        assertTrue(settings.isMonitoringEnabled(SourceApp.MANUAL))
    }

    @Test
    fun `other is monitored only if at least one channel is on`() {
        assertFalse(
            AppSettings(
                monitorSms = false,
                monitorWhatsapp = false,
                monitorTelegram = false,
                monitorInstagram = false,
                monitorMessenger = false,
                monitorViber = false,
                monitorVk = false,
                monitorOk = false,
            ).isMonitoringEnabled(SourceApp.OTHER),
        )
        assertTrue(
            AppSettings(monitorSms = true).isMonitoringEnabled(SourceApp.OTHER),
        )
        assertTrue(
            AppSettings(
                monitorSms = false,
                monitorWhatsapp = false,
                monitorTelegram = false,
                monitorInstagram = true,
            ).isMonitoringEnabled(SourceApp.OTHER),
        )
    }
}
