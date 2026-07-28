package kz.qorgau.scamguardian.notification

import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.SourceApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeduperTest {

    @Test
    fun `duplicate within window is rejected`() {
        var now = 1_000L
        val deduper = NotificationDeduper(windowMs = 30_000L, nowMs = { now })
        val message = sample("key-1", "hello scam")

        assertTrue(deduper.shouldProcess(message))
        assertFalse(deduper.shouldProcess(message))

        now += 31_000L
        assertTrue(deduper.shouldProcess(message))
    }

    @Test
    fun `different keys are both accepted`() {
        val deduper = NotificationDeduper(nowMs = { 5_000L })
        assertTrue(deduper.shouldProcess(sample("a", "one")))
        assertTrue(deduper.shouldProcess(sample("b", "two")))
    }

    private fun sample(key: String, text: String) = IncomingMessage(
        sourceApp = SourceApp.SMS,
        packageName = "com.android.mms",
        sender = "bank",
        text = text,
        receivedAtEpochMs = 1L,
        notificationKey = key,
    )
}
