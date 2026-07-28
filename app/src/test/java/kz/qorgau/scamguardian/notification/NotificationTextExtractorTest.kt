package kz.qorgau.scamguardian.notification

import kz.qorgau.scamguardian.domain.model.SourceApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `strips whatsapp name prefix when matches title`() {
        val cleaned = NotificationTextExtractor.cleanupBody(
            body = "Alice: Привет, переведи мне пожалуйста",
            title = "Alice",
            sourceApp = SourceApp.WHATSAPP,
        )
        assertEquals("Привет, переведи мне пожалуйста", cleaned)
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
    fun `ignores group conversations flag`() {
        val result = NotificationTextExtractor.extractFromParts(
            sourceApp = SourceApp.TELEGRAM,
            packageName = "org.telegram.messenger",
            title = "Family",
            text = "Hello everyone",
            bigText = null,
            isGroupConversation = true,
            receivedAtEpochMs = 1L,
        )
        assertEquals(
            NotificationTextExtractor.IgnoreReason.GROUP_CHAT,
            (result as NotificationTextExtractor.ExtractResult.Ignored).reason,
        )
    }

    @Test
    fun `detects multi-sender group preview heuristically`() {
        val isGroup = NotificationTextExtractor.isGroupConversation(
            sourceApp = SourceApp.WHATSAPP,
            title = "Work Chat",
            text = "Alice: hi\nBob: ok\nCarol: thanks",
        )
        assertTrue(isGroup)
    }

    @Test
    fun `detects participant count in title as group`() {
        val isGroup = NotificationTextExtractor.isGroupConversation(
            sourceApp = SourceApp.WHATSAPP,
            title = "Family (12)",
            text = "hello",
        )
        assertTrue(isGroup)
    }

    @Test
    fun `one-to-one chat is not treated as group`() {
        val isGroup = NotificationTextExtractor.isGroupConversation(
            sourceApp = SourceApp.TELEGRAM,
            title = "Alice",
            text = "Привет, как дела?",
            flaggedAsGroup = false,
        )
        assertFalse(isGroup)
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
}
