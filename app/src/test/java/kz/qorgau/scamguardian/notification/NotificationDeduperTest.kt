package kz.qorgau.scamguardian.notification

import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.SourceApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeduperTest {

    @Test
    fun `duplicate same key and text within window is rejected`() {
        var now = 1_000L
        val deduper = NotificationDeduper(windowMs = 30_000L, nowMs = { now })
        val message = sample("key-1", "hello scam")

        assertTrue(deduper.shouldProcess(message))
        assertFalse(deduper.shouldProcess(message))

        now += 31_000L
        assertTrue(deduper.shouldProcess(message))
    }

    @Test
    fun `same key with new text is accepted`() {
        val deduper = NotificationDeduper(nowMs = { 5_000L })
        assertTrue(deduper.shouldProcess(sample("same", "first body")))
        assertTrue(deduper.shouldProcess(sample("same", "updated body with code")))
    }

    @Test
    fun `same text under different notification keys is rejected`() {
        val deduper = NotificationDeduper(nowMs = { 5_000L })
        assertTrue(deduper.shouldProcess(sample("key-a", "құтты болсын", sender = "F4™")))
        // WhatsApp / OEM often re-post the same body with another key.
        assertFalse(deduper.shouldProcess(sample("key-b", "құтты болсын", sender = "F4™")))
    }

    @Test
    fun `same text with message-count sender suffix is rejected`() {
        val deduper = NotificationDeduper(nowMs = { 5_000L })
        assertTrue(deduper.shouldProcess(sample("k1", "Рахмат", sender = "F4™")))
        assertFalse(
            deduper.shouldProcess(
                sample("k2", "Рахмат", sender = "F4™ (2 messages)"),
            ),
        )
    }

    @Test
    fun `different senders with same text are both accepted`() {
        val deduper = NotificationDeduper(nowMs = { 5_000L })
        assertTrue(deduper.shouldProcess(sample("a", "ok", sender = "Alice")))
        assertTrue(deduper.shouldProcess(sample("b", "ok", sender = "Bob")))
    }

    @Test
    fun `different keys with different text are both accepted`() {
        val deduper = NotificationDeduper(nowMs = { 5_000L })
        assertTrue(deduper.shouldProcess(sample("a", "one")))
        assertTrue(deduper.shouldProcess(sample("b", "two")))
    }

    @Test
    fun `normalizeSenderForDedup strips message count`() {
        assertEquals("F4™", NotificationDeduper.normalizeSenderForDedup("F4™ (2 messages)"))
        assertEquals("F4™", NotificationDeduper.normalizeSenderForDedup("F4™ (2 сообщения)"))
        assertEquals("Alice", NotificationDeduper.normalizeSenderForDedup("Alice"))
        assertEquals("", NotificationDeduper.normalizeSenderForDedup(null))
    }

    private fun sample(
        key: String,
        text: String,
        sender: String = "bank",
    ) = IncomingMessage(
        sourceApp = SourceApp.SMS,
        packageName = "com.android.mms",
        sender = sender,
        text = text,
        receivedAtEpochMs = 1L,
        notificationKey = key,
    )
}
