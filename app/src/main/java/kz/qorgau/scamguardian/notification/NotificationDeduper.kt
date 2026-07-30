package kz.qorgau.scamguardian.notification

import kz.qorgau.scamguardian.domain.model.IncomingMessage

/**
 * In-memory dedup for duplicate notification posts.
 *
 * Fingerprint is **content-based** (source + package + sender + text), not notification key.
 * Messaging apps often re-post the same body under a new key (group summary, NLS reconnect,
 * OEM cancel+repost). Key-only dedup allowed those through and flooded History.
 *
 * Same conversation key with **changed text** is still allowed (WhatsApp updates in place).
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
        val sender = normalizeSenderForDedup(message.sender)
        val text = message.text.trim()
        // Content identity — ignore notificationKey so re-posts of the same body are dropped.
        return listOf(
            message.sourceApp.storageValue,
            message.packageName,
            sender,
            text,
        ).joinToString("\u0000")
    }

    companion object {
        /**
         * Strip WhatsApp-style " (2 messages)" / " (2 сообщения)" suffixes so the same chat
         * matches whether the shade shows a single message or a multi-message title.
         */
        fun normalizeSenderForDedup(sender: String?): String {
            if (sender.isNullOrBlank()) return ""
            return SENDER_COUNT_SUFFIX.replace(sender.trim(), "").trim()
        }

        private val SENDER_COUNT_SUFFIX = Regex(
            """\s*\(\d+\s+[^)]{1,40}\)\s*$""",
        )
    }
}
