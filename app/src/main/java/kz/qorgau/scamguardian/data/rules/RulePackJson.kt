package kz.qorgau.scamguardian.data.rules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.RulePack
import kz.qorgau.scamguardian.domain.model.ScamRule

@Serializable
internal data class RulePackDto(
    val version: String,
    val name: String = "unnamed",
    val description: String = "",
    val rules: List<RuleDto>,
)

@Serializable
internal data class RuleDto(
    val id: String,
    val category: String,
    val languages: List<String> = listOf("both"),
    @SerialName("severity_weight")
    val severityWeight: Float,
    val match: String = "any",
    val patterns: List<String>,
    @SerialName("description_ru")
    val descriptionRu: String,
    @SerialName("description_kk")
    val descriptionKk: String,
)

internal fun RulePackDto.toDomain(): RulePack =
    RulePack(
        version = version,
        name = name,
        description = description,
        rules = rules.map { it.toDomain() },
    )

internal fun RuleDto.toDomain(): ScamRule =
    ScamRule(
        id = id,
        descriptionRu = descriptionRu,
        descriptionKk = descriptionKk,
        languages = languages.toLanguageSet(),
        severityWeight = severityWeight,
        patterns = patterns,
        category = category,
        matchMode = when (match.lowercase()) {
            "all" -> ScamRule.MatchMode.ALL
            else -> ScamRule.MatchMode.ANY
        },
    )

private fun List<String>.toLanguageSet(): Set<AppLanguage> {
    val tags = map { it.lowercase() }.toSet()
    if (tags.contains("both") || tags.containsAll(listOf("ru", "kk"))) {
        return setOf(AppLanguage.RUSSIAN, AppLanguage.KAZAKH)
    }
    return buildSet {
        if ("ru" in tags) add(AppLanguage.RUSSIAN)
        if ("kk" in tags) add(AppLanguage.KAZAKH)
        if (isEmpty()) {
            add(AppLanguage.RUSSIAN)
            add(AppLanguage.KAZAKH)
        }
    }
}
