package kz.qorgau.scamguardian.notification

/**
 * Resource / spam limits for notification capture (ARCHITECTURE.md §3.1).
 */
object NotificationCaptureConfig {
    /** Ignore messages longer than this (chars) to save CPU/battery. */
    const val MAX_MESSAGE_LENGTH: Int = 2_000

    /** Minimum useful text length after cleanup. */
    const val MIN_MESSAGE_LENGTH: Int = 4

    /** Dedup window for the same notification content (ms). */
    const val DEDUP_WINDOW_MS: Long = 30_000L

    /** Max entries kept in the in-memory dedup set. */
    const val DEDUP_CACHE_SIZE: Int = 64
}
