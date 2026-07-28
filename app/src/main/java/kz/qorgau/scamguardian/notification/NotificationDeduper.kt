package kz.qorgau.scamguardian.notification

import kz.qorgau.scamguardian.domain.model.IncomingMessage

/**
 * In-memory dedup for duplicate notification posts.
 * Same notification key with **changed text** is allowed (WhatsApp updates in place).
 */
class NotificationDeduper(
    private val windowMs: Long = NotificationCaptureConfig.DEDUP_WINDOW_MS,
    private val maxSize: Int = NotificationCaptureConfig.DEDUP_CACHE_SIZE,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private data class Entry(val fingerprint: String, val seenAt: Long)

    private val recent = ArrayDeque<Entry>()

    @Synchronized
    fun shouldProcess(message: IncomingMessage): Boolean {
        val now = nowMs()
        prune(now)
        val fingerprint = fingerprintOf(message)
        if (recent.any { it.fingerprint == fingerprint }) {
            return false
        }
        recent.addLast(Entry(fingerprint, now))
        while (recent.size > maxSize) {
            recent.removeFirst()
        }
        return true
    }

    private fun prune(now: Long) {
        while (recent.isNotEmpty() && now - recent.first().seenAt > windowMs) {
            recent.removeFirst()
        }
    }

    private fun fingerprintOf(message: IncomingMessage): String {
        val sample = message.text.take(160).hashCode()
        val key = message.notificationKey
        // Include text hash so content updates on the same key still run.
        return if (key != null) {
            "key:$key|$sample"
        } else {
            "${message.sourceApp.storageValue}|${message.sender}|$sample"
        }
    }
}
