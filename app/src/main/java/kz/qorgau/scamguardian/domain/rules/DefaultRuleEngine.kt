package kz.qorgau.scamguardian.domain.rules

import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.RulePack
import kz.qorgau.scamguardian.domain.model.ScamRule
import kz.qorgau.scamguardian.domain.model.Sensitivity

/**
 * Fast deterministic rule engine (ARCHITECTURE.md §3.2, RULES.md §4).
 *
 * - Rules are data ([ScamRule]), not scattered if-statements.
 * - Evaluation is pure (no side effects).
 * - Target path: compile patterns once, match on normalized text.
 */
class DefaultRuleEngine(
    private val rules: List<ScamRule>,
    private val packVersion: String = "unknown",
) : RuleEngine {

    constructor(pack: RulePack) : this(pack.rules, pack.version)

    private val compiled: List<CompiledRule> = rules.map { rule ->
        CompiledRule(
            rule = rule,
            matchers = rule.patterns.map { PatternMatcher.compile(it) },
        )
    }

    val version: String get() = packVersion

    val ruleCount: Int get() = compiled.size

    override fun evaluate(
        text: String,
        language: AppLanguage,
        sensitivity: Sensitivity,
    ): RuleEvaluationResult {
        val normalized = TextNormalizer.normalize(text)
        if (normalized.isEmpty()) {
            return RuleEvaluationResult(
                riskLevel = RiskLevel.SAFE,
                matchedRuleIds = emptyList(),
                explanation = explanationSafe(language),
                confidence = 1f,
                isUncertain = false,
            )
        }

        // Always evaluate every rule. [language] only selects explanation copy,
        // not which patterns run — scams arrive in RU/KK regardless of UI language.
        val matched = compiled.mapNotNull { compiledRule ->
            val rule = compiledRule.rule
            if (!compiledRule.matches(normalized)) return@mapNotNull null
            rule
        }

        if (matched.isEmpty()) {
            return RuleEvaluationResult(
                riskLevel = RiskLevel.SAFE,
                matchedRuleIds = emptyList(),
                explanation = explanationSafe(language),
                confidence = 0.55f,
                isUncertain = false,
            )
        }

        val totalWeight = matched.sumOf { it.severityWeight.toDouble() }.toFloat()
        val topRules = matched.sortedByDescending { it.severityWeight }
        val thresholds = Thresholds.forSensitivity(sensitivity)
        val riskLevel = when {
            totalWeight >= thresholds.high -> RiskLevel.HIGH
            totalWeight >= thresholds.suspicious -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.SAFE
        }

        // Ambiguous: some signal, but not clearly high.
        val isUncertain = when (riskLevel) {
            RiskLevel.SUSPICIOUS -> true
            RiskLevel.SAFE -> totalWeight >= thresholds.uncertainFloor
            RiskLevel.HIGH -> false
        }

        val confidence = when (riskLevel) {
            RiskLevel.HIGH -> (0.75f + totalWeight * 0.15f).coerceAtMost(0.99f)
            RiskLevel.SUSPICIOUS -> (0.45f + totalWeight * 0.2f).coerceIn(0.4f, 0.8f)
            RiskLevel.SAFE -> 0.5f
        }

        return RuleEvaluationResult(
            riskLevel = riskLevel,
            matchedRuleIds = topRules.map { it.id },
            explanation = buildExplanation(topRules, riskLevel, language),
            confidence = confidence,
            isUncertain = isUncertain,
        )
    }

    private fun buildExplanation(
        matched: List<ScamRule>,
        riskLevel: RiskLevel,
        language: AppLanguage,
    ): String {
        val reasons = matched.take(3).joinToString(separator = " ") { rule ->
            rule.descriptionFor(language)
        }
        val header = when (language) {
            AppLanguage.RUSSIAN -> when (riskLevel) {
                RiskLevel.HIGH -> "Похоже на мошенничество."
                RiskLevel.SUSPICIOUS -> "Сообщение выглядит подозрительно."
                RiskLevel.SAFE -> "Явных признаков скама не найдено."
            }
            AppLanguage.KAZAKH -> when (riskLevel) {
                RiskLevel.HIGH -> "Бұл алаяқтыққа ұқсайды."
                RiskLevel.SUSPICIOUS -> "Хабарлама күдікті көрінеді."
                RiskLevel.SAFE -> "Айқын алаяқтық белгілері табылмады."
            }
            AppLanguage.ENGLISH -> when (riskLevel) {
                RiskLevel.HIGH -> "This looks like a scam."
                RiskLevel.SUSPICIOUS -> "This message looks suspicious."
                RiskLevel.SAFE -> "No clear scam signs found."
            }
        }
        return if (riskLevel == RiskLevel.SAFE) {
            header
        } else {
            "$header $reasons".trim()
        }
    }

    private fun explanationSafe(language: AppLanguage): String =
        when (language) {
            AppLanguage.RUSSIAN -> "Явных признаков скама не найдено."
            AppLanguage.KAZAKH -> "Айқын алаяқтық белгілері табылмады."
            AppLanguage.ENGLISH -> "No clear scam signs found."
        }

    private data class CompiledRule(
        val rule: ScamRule,
        val matchers: List<PatternMatcher>,
    ) {
        fun matches(normalizedText: String): Boolean =
            when (rule.matchMode) {
                ScamRule.MatchMode.ALL -> matchers.all { it.matches(normalizedText) }
                ScamRule.MatchMode.ANY -> matchers.any { it.matches(normalizedText) }
            }
    }

    private data class Thresholds(
        val high: Float,
        val suspicious: Float,
        val uncertainFloor: Float,
    ) {
        companion object {
            fun forSensitivity(sensitivity: Sensitivity): Thresholds =
                when (sensitivity) {
                    // Slightly lower than MEDIUM so single weak bait phrases (0.26–0.31)
                    // can tip to SUSPICIOUS at maximum sensitivity.
                    Sensitivity.HIGH -> Thresholds(
                        high = 0.40f,
                        suspicious = 0.20f,
                        uncertainFloor = 0.15f,
                    )
                    Sensitivity.MEDIUM -> Thresholds(
                        high = 0.55f,
                        suspicious = 0.32f,
                        uncertainFloor = 0.22f,
                    )
                    Sensitivity.LOW -> Thresholds(
                        high = 0.70f,
                        suspicious = 0.45f,
                        uncertainFloor = 0.30f,
                    )
                }
        }
    }
}

/**
 * Pattern matcher: plain substring (default) or `regex:...` for advanced cases.
 * Compiled once per rule pack load.
 */
internal sealed class PatternMatcher {
    abstract fun matches(normalizedText: String): Boolean

    private class SubstringMatcher(private val needle: String) : PatternMatcher() {
        override fun matches(normalizedText: String): Boolean =
            needle.isNotEmpty() && needle in normalizedText
    }

    private class RegexMatcher(private val regex: Regex) : PatternMatcher() {
        override fun matches(normalizedText: String): Boolean =
            regex.containsMatchIn(normalizedText)
    }

    companion object {
        private const val REGEX_PREFIX = "regex:"

        fun compile(rawPattern: String): PatternMatcher {
            val pattern = rawPattern.trim()
            return if (pattern.startsWith(REGEX_PREFIX)) {
                val body = pattern.removePrefix(REGEX_PREFIX)
                RegexMatcher(
                    Regex(body, setOf(RegexOption.IGNORE_CASE)),
                )
            } else {
                SubstringMatcher(TextNormalizer.normalize(pattern))
            }
        }
    }
}
