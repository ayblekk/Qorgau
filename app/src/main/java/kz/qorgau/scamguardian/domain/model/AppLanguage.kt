package kz.qorgau.scamguardian.domain.model

/**
 * UI + explanation language (PRD F6, SCHEMA.md §3.2).
 */
enum class AppLanguage(val storageValue: String) {
    RUSSIAN("ru"),
    KAZAKH("kk"),
    ENGLISH("en");

    companion object {
        fun fromStorage(value: String): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: RUSSIAN
    }
}
