package kz.qorgau.scamguardian.notification

/**
 * Resource / spam limits for notification capture (ARCHITECTURE.md §3.1).
 */
object NotificationCaptureConfig {
    /** Ignore messages longer than this (chars) to save CPU/battery. */
    const val MAX_MESSAGE_LENGTH: Int = 2_000

    /** Minimum useful text length after cleanup. */
    const val MIN_MESSAGE_LENGTH: Int = 4

    /**
     * In-memory dedup window for the same message content (ms).
     * Covers OEM double-posts and short NLS reconnects.
     */
    const val DEDUP_WINDOW_MS: Long = 120_000L

    /**
     * Persistent (DB) time-proximity window: treat same source+sender+text as a duplicate
     * when |stored.created_at − incoming.receivedAt| is within this range.
     * Reprocess of shade notifications reuses the original postTime, so ABS ≈ 0.
     */
    const val DB_DEDUP_PROXIMITY_MS: Long = 5 * 60_000L

    /** Max entries kept in the in-memory dedup set. */
    const val DEDUP_CACHE_SIZE: Int = 64
}
