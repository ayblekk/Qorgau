package kz.qorgau.scamguardian.domain.model

/**
 * Local-only user feedback on an analysis entry (SCHEMA.md §3.1).
 * Never leaves the device.
 */
enum class UserFeedback(val storageValue: String) {
    FALSE_POSITIVE("false_positive"),
    CONFIRMED("confirmed");

    companion object {
        fun fromStorage(value: String?): UserFeedback? =
            value?.let { stored ->
                entries.firstOrNull { it.storageValue == stored }
            }
    }
}
