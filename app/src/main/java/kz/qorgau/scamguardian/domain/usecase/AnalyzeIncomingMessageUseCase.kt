package kz.qorgau.scamguardian.domain.usecase

import kz.qorgau.scamguardian.domain.classifier.ClassifierResult
import kz.qorgau.scamguardian.domain.classifier.ScamClassifier
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.Sensitivity
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.domain.rules.RuleEngine
import kz.qorgau.scamguardian.domain.rules.RuleEvaluationResult

/**
 * Core on-device analysis path: rules first, optional classifier on uncertain cases.
 * Never performs network I/O. Fail-safe: classifier null/timeout → rules result.
 */
class AnalyzeIncomingMessageUseCase(
    private val ruleEngine: RuleEngine,
    private val analysisRepository: AnalysisRepository,
    private val settingsRepository: SettingsRepository,
    private val scamClassifier: ScamClassifier,
) {

    data class Outcome(
        val record: AnalysisRecord,
        val shouldAlert: Boolean,
        val usedClassifier: Boolean = false,
    )

    /**
     * @return null when monitoring for this source is disabled.
     */
    suspend fun execute(message: IncomingMessage): Outcome? {
        val settings = settingsRepository.getSettings()
        if (!settings.isMonitoringEnabled(message.sourceApp)) {
            return null
        }
        return analyzeAndStore(message, settings)
    }

    /**
     * Manual check ignores per-app monitor toggles.
     */
    suspend fun executeManual(message: IncomingMessage): Outcome {
        val settings = settingsRepository.getSettings()
        return analyzeAndStore(message, settings)
    }

    private suspend fun analyzeAndStore(
        message: IncomingMessage,
        settings: AppSettings,
    ): Outcome {
        val evaluation = ruleEngine.evaluate(
            text = message.text,
            language = settings.language,
            sensitivity = settings.sensitivity,
        )

        var riskLevel = evaluation.riskLevel
        var riskScore: Float? = evaluation.confidence
        var explanation = evaluation.explanation
        var usedClassifier = false

        if (shouldInvokeClassifier(settings, evaluation)) {
            val classified = scamClassifier.classify(message.text)
            if (classified != null) {
                usedClassifier = true
                val merged = mergeWithClassifier(
                    rules = evaluation,
                    classified = classified,
                    language = settings.language,
                )
                riskLevel = merged.riskLevel
                riskScore = merged.riskScore
                explanation = merged.explanation
            }
            // else: timeout / unavailable → keep pure rules (fail safe)
        }

        val record = AnalysisRecord(
            sourceApp = message.sourceApp,
            sender = message.sender,
            messageText = message.text,
            riskLevel = riskLevel,
            riskScore = riskScore,
            explanation = explanation,
            matchedRules = evaluation.matchedRuleIds,
            createdAtEpochMs = message.receivedAtEpochMs,
            isRead = false,
        )

        val id = analysisRepository.insert(record)
        val stored = record.copy(id = id)

        val shouldAlert = riskLevel == RiskLevel.HIGH ||
            (riskLevel == RiskLevel.SUSPICIOUS && settings.sensitivity != Sensitivity.LOW)

        return Outcome(
            record = stored,
            shouldAlert = shouldAlert,
            usedClassifier = usedClassifier,
        )
    }

    /**
     * Classifier only when rules are uncertain and user/settings allow model path.
     */
    internal fun shouldInvokeClassifier(
        settings: AppSettings,
        evaluation: RuleEvaluationResult,
    ): Boolean {
        if (!evaluation.isUncertain) return false
        if (settings.rulesOnlyMode) return false
        if (!settings.modelEnabled) return false
        if (!scamClassifier.isAvailable) return false
        return true
    }

    /**
     * Classifier decides among uncertain cases; never lowers a clear HIGH from rules.
     */
    internal fun mergeWithClassifier(
        rules: RuleEvaluationResult,
        classified: ClassifierResult,
        language: AppLanguage,
    ): MergedDecision {
        val fromScore = riskFromScore(classified.riskScore)
        val riskLevel = maxRisk(rules.riskLevel, fromScore)
        val explanation = classified.explanation.ifBlank {
            rules.explanation.ifBlank { defaultExplanation(riskLevel, language) }
        }
        return MergedDecision(
            riskLevel = riskLevel,
            riskScore = classified.riskScore,
            explanation = explanation,
        )
    }

    private fun riskFromScore(score: Float): RiskLevel =
        when {
            score >= 0.75f -> RiskLevel.HIGH
            score >= 0.45f -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.SAFE
        }

    private fun maxRisk(a: RiskLevel, b: RiskLevel): RiskLevel {
        // HIGH < SUSPICIOUS < SAFE in ordinal (enum declaration order).
        return if (a.ordinal <= b.ordinal) a else b
    }

    private fun defaultExplanation(level: RiskLevel, language: AppLanguage): String =
        when (language) {
            AppLanguage.RUSSIAN -> when (level) {
                RiskLevel.HIGH -> "Модель оценила сообщение как высокий риск."
                RiskLevel.SUSPICIOUS -> "Модель оценила сообщение как подозрительное."
                RiskLevel.SAFE -> "Модель не нашла явных признаков скама."
            }
            AppLanguage.KAZAKH -> when (level) {
                RiskLevel.HIGH -> "Модель хабарламаны жоғары қауіп деп бағалады."
                RiskLevel.SUSPICIOUS -> "Модель хабарламаны күдікті деп бағалады."
                RiskLevel.SAFE -> "Модель алаяқтықтың айқын белгілерін таппады."
            }
        }

    data class MergedDecision(
        val riskLevel: RiskLevel,
        val riskScore: Float,
        val explanation: String,
    )
}
