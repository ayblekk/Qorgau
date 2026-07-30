package kz.qorgau.scamguardian.notification

import kz.qorgau.scamguardian.domain.model.SourceApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTextExtractorTest {

    @Test
    fun `extracts sms body with sender title`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.SMS,
            packageName = "com.google.android.apps.messaging",
            title = "+77001234567",
            text = "Kaspi: срочно пришлите код из СМС",
            bigText = null,
            receivedAtEpochMs = 1_700_000_000_000L,
        )
        assertTrue(result is NotificationTextExtractor.ExtractResult.Success)
        val msg = (result as NotificationTextExtractor.ExtractResult.Success).message
        assertEquals(SourceApp.SMS, msg.sourceApp)
        assertEquals("+77001234567", msg.sender)
        assertTrue(msg.text.contains("код"))
    }

    @Test
    fun `prefers bigText over text`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.TELEGRAM,
            packageName = "org.telegram.messenger",
            title = "Support",
            text = "short",
            bigText = "Full long message about AnyDesk and bank account",
            receivedAtEpochMs = 1L,
        )
        val msg = (result as NotificationTextExtractor.ExtractResult.Success).message
        assertTrue(msg.text.contains("Full long message"))
    }

    @Test
    fun `prefers messaging style over short text label`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.WHATSAPP,
            packageName = "com.whatsapp",
            title = "Марлен",
            text = "Марлен",
            bigText = null,
            messagingLines = listOf(
                "Здравствуйте. Это служба безопасности банка. Сообщите код из SMS",
            ),
            receivedAtEpochMs = 1L,
        )
        val msg = (result as NotificationTextExtractor.ExtractResult.Success).message
        assertTrue(msg.text.contains("служба безопасности"))
        assertTrue(msg.text.contains("код"))
    }

    @Test
    fun `strips whatsapp name prefix when matches title`() {
        val cleaned = NotificationTextExtractor.cleanupBody(
            body = "Alice: Привет, переведи мне пожалуйста",
            title = "Alice",
            sourceApp = SourceApp.WHATSAPP,
        )
        assertEquals("Привет, переведи мне пожалуйста", cleaned)
    }

    @Test
    fun `strips invisible ltr marks used by whatsapp`() {
        val cleaned = NotificationTextExtractor.cleanupBody(
            body = "\u200EМарлен: код из SMS",
            title = "Марлен",
            sourceApp = SourceApp.WHATSAPP,
        )
        assertEquals("код из SMS", cleaned)
    }

    @Test
    fun `ignores empty body`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.WHATSAPP,
            packageName = "com.whatsapp",
            title = "Bob",
            text = "  ",
            bigText = null,
            receivedAtEpochMs = 1L,
        )
        assertEquals(
            NotificationTextExtractor.IgnoreReason.EMPTY_TEXT,
            (result as NotificationTextExtractor.ExtractResult.Ignored).reason,
        )
    }

    @Test
    fun `ignores too long messages`() {
        val longText = "a".repeat(NotificationCaptureConfig.MAX_MESSAGE_LENGTH + 1)
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.SMS,
            packageName = "com.android.mms",
            title = "svc",
            text = longText,
            bigText = null,
            receivedAtEpochMs = 1L,
        )
        assertEquals(
            NotificationTextExtractor.IgnoreReason.TOO_LONG,
            (result as NotificationTextExtractor.ExtractResult.Ignored).reason,
        )
    }

    @Test
    fun `group conversations are still analyzed`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.TELEGRAM,
            packageName = "org.telegram.messenger",
            title = "Family",
            text = "Hello everyone transfer money now",
            bigText = null,
            isGroupConversation = true,
            receivedAtEpochMs = 1L,
        )
        assertTrue(result is NotificationTextExtractor.ExtractResult.Success)
        val msg = (result as NotificationTextExtractor.ExtractResult.Success).message
        assertTrue(msg.isGroupConversation)
    }

    @Test
    fun `uses last inbox text line when body empty`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.SMS,
            packageName = "com.android.mms",
            title = "Messages",
            text = null,
            bigText = null,
            textLines = listOf("old msg", "Kaspi: пришлите код"),
            receivedAtEpochMs = 1L,
        )
        val msg = (result as NotificationTextExtractor.ExtractResult.Success).message
        assertTrue(msg.text.contains("код"))
    }

    @Test
    fun `uses messaging style lines when present`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.WHATSAPP,
            packageName = "com.whatsapp",
            title = "Bank Support",
            text = null,
            bigText = null,
            messagingLines = listOf("Установите AnyDesk для проверки перевода"),
            receivedAtEpochMs = 1L,
        )
        val msg = (result as NotificationTextExtractor.ExtractResult.Success).message
        assertTrue(msg.text.contains("AnyDesk"))
    }

    @Test
    fun `ignores useless whatsapp summary labels`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.WHATSAPP,
            packageName = "com.whatsapp",
            title = "WhatsApp",
            text = "5 new messages",
            bigText = null,
            isGroupSummary = true,
            receivedAtEpochMs = 1L,
        )
        assertEquals(
            NotificationTextExtractor.IgnoreReason.USELESS_SUMMARY,
            (result as NotificationTextExtractor.ExtractResult.Ignored).reason,
        )
    }

    @Test
    fun `ignores message from phone placeholder`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.SMS,
            packageName = "com.android.mms",
            title = "+7 707 028 1515",
            text = "Message from +7 707 028 1515",
            bigText = null,
            receivedAtEpochMs = 1L,
        )
        assertEquals(
            NotificationTextExtractor.IgnoreReason.USELESS_SUMMARY,
            (result as NotificationTextExtractor.ExtractResult.Ignored).reason,
        )
    }

    @Test
    fun `ignores russian message from placeholder`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.WHATSAPP,
            packageName = "com.whatsapp",
            title = "Али",
            text = "Сообщение от Али",
            bigText = null,
            receivedAtEpochMs = 1L,
        )
        assertEquals(
            NotificationTextExtractor.IgnoreReason.USELESS_SUMMARY,
            (result as NotificationTextExtractor.ExtractResult.Ignored).reason,
        )
    }

    @Test
    fun `ignores phone-only body with phone title`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.SMS,
            packageName = "com.google.android.apps.messaging",
            title = "+77070281515",
            text = "+77070281515",
            bigText = null,
            receivedAtEpochMs = 1L,
        )
        assertEquals(
            NotificationTextExtractor.IgnoreReason.USELESS_SUMMARY,
            (result as NotificationTextExtractor.ExtractResult.Ignored).reason,
        )
    }

    @Test
    fun `prefers real body over message from placeholder candidate`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.SMS,
            packageName = "com.android.mms",
            title = "+7 707 028 1515",
            text = "Message from +7 707 028 1515",
            bigText = "В наличии калмады",
            receivedAtEpochMs = 1L,
        )
        assertTrue(result is NotificationTextExtractor.ExtractResult.Success)
        val msg = (result as NotificationTextExtractor.ExtractResult.Success).message
        assertTrue(msg.text.contains("калмады"))
    }

    @Test
    fun `uses ticker as fallback body`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.SMS,
            packageName = "com.android.mms",
            title = "Bank",
            text = null,
            bigText = null,
            ticker = "Kaspi: подтвердите перевод 10000",
            receivedAtEpochMs = 1L,
        )
        val msg = (result as NotificationTextExtractor.ExtractResult.Success).message
        assertTrue(msg.text.contains("подтвердите") || msg.text.contains("перевод"))
    }
}
