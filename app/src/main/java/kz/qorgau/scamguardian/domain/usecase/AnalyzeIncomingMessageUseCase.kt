package kz.qorgau.scamguardian.domain.usecase

import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.Sensitivity
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.domain.rules.RuleEngine

/**
 * Core on-device analysis path: rule-engine only.
 * Never performs network I/O.
 */
class AnalyzeIncomingMessageUseCase(
    private val ruleEngine: RuleEngine,
    private val analysisRepository: AnalysisRepository,
    private val settingsRepository: SettingsRepository,
) {

    data class Outcome(
        val record: AnalysisRecord,
        val shouldAlert: Boolean,
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

        val record = AnalysisRecord(
            sourceApp = message.sourceApp,
            sender = message.sender,
            messageText = message.text,
            riskLevel = evaluation.riskLevel,
            riskScore = evaluation.confidence,
            explanation = evaluation.explanation,
            matchedRules = evaluation.matchedRuleIds,
            createdAtEpochMs = message.receivedAtEpochMs,
            isRead = false,
        )

        val id = analysisRepository.insert(record)
        val stored = record.copy(id = id)

        val shouldAlert = evaluation.riskLevel == RiskLevel.HIGH ||
            (evaluation.riskLevel == RiskLevel.SUSPICIOUS && settings.sensitivity != Sensitivity.LOW)

        return Outcome(
            record = stored,
            shouldAlert = shouldAlert,
        )
    }
}
