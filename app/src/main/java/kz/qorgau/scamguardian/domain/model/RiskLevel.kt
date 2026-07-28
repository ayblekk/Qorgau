package kz.qorgau.scamguardian.domain.model

/**
 * Risk classification for an analyzed message (SCHEMA.md §3.1).
 */
enum class RiskLevel(val storageValue: String) {
    HIGH("high"),
    SUSPICIOUS("suspicious"),
    SAFE("safe");

    companion object {
        fun fromStorage(value: String): RiskLevel =
            entries.firstOrNull { it.storageValue == value }
                ?: error("Unknown risk_level: $value")
    }
}
