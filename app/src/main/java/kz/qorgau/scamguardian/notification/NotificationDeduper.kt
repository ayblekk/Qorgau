package kz.qorgau.scamguardian.notification

import kz.qorgau.scamguardian.domain.model.IncomingMessage

/**
 * In-memory dedup for duplicate notification posts (same content within a short window).
 * Does not store message text beyond a hash fingerprint.
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
        // Prefer platform key when present (stable per notification update).
        message.notificationKey?.let { key ->
            return "key:$key"
        }
        val sample = message.text.take(120)
        return "${message.sourceApp.storageValue}|${message.sender}|${sample.hashCode()}"
    }
}
