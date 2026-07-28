package kz.qorgau.scamguardian.domain.model

/**
 * Structured rule definition (RULES.md §4 — rules are data, not scattered ifs).
 */
data class ScamRule(
    val id: String,
    val descriptionRu: String,
    val descriptionKk: String,
    val descriptionEn: String,
    val languages: Set<AppLanguage>,
    val severityWeight: Float,
    val patterns: List<String>,
    val category: String,
    /** `any` = at least one pattern; `all` = every pattern must match. */
    val matchMode: MatchMode = MatchMode.ANY,
) {
    enum class MatchMode {
        ANY,
        ALL,
    }

    fun descriptionFor(language: AppLanguage): String =
        when (language) {
            AppLanguage.KAZAKH -> descriptionKk
            AppLanguage.ENGLISH -> descriptionEn.ifBlank { descriptionRu }
            AppLanguage.RUSSIAN -> descriptionRu
        }
}

/**
 * Versioned rule pack (SCHEMA.md §6 — packs versioned independently).
 */
data class RulePack(
    val version: String,
    val name: String,
    val description: String,
    val rules: List<ScamRule>,
)
