package kz.qorgau.scamguardian.domain.rules

import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.Sensitivity

/**
 * Fast deterministic pattern matching (ARCHITECTURE.md §3.2, RULES.md §4).
 * Implementation lands in the next Stage 1 block.
 */
interface RuleEngine {
    fun evaluate(
        text: String,
        language: AppLanguage,
        sensitivity: Sensitivity,
    ): RuleEvaluationResult
}

data class RuleEvaluationResult(
    val riskLevel: RiskLevel,
    val matchedRuleIds: List<String>,
    val explanation: String,
    val confidence: Float,
    /** True when rules are inconclusive and classifier may help. */
    val isUncertain: Boolean,
)
