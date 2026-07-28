package kz.qorgau.scamguardian.domain.model

/**
 * Structured rule definition (RULES.md §4 — rules are data, not scattered ifs).
 * Loaded from JSON assets or built-in packs in a later stage block.
 */
data class ScamRule(
    val id: String,
    val descriptionRu: String,
    val descriptionKk: String,
    val languages: Set<AppLanguage>,
    val severityWeight: Float,
    val patterns: List<String>,
    val category: String,
)
