package kz.qorgau.scamguardian.domain.model

/**
 * User-facing detection sensitivity (SCHEMA.md §3.2).
 */
enum class Sensitivity(val storageValue: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    companion object {
        fun fromStorage(value: String): Sensitivity =
            entries.firstOrNull { it.storageValue == value } ?: MEDIUM
    }
}
