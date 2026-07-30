package kz.qorgau.scamguardian.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.Sensitivity
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.domain.rules.OfficialSenderPolicy
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

    /** Serializes check-then-insert so concurrent NLS callbacks cannot double-store. */
    private val storeMutex = Mutex()

    data class Outcome(
        val record: AnalysisRecord,
        val shouldAlert: Boolean,
        /** True when this was a re-post of content already stored (no new History row). */
        val wasDuplicate: Boolean = false,
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
    ): Outcome = storeMutex.withLock {
        // Survive process death + NLS reprocessActiveNotifications: same shade
        // notification reuses postTime, so ABS(created_at − receivedAt) ≈ 0.
        val existing = analysisRepository.findRecentDuplicate(
            sourceApp = message.sourceApp,
            sender = message.sender,
            messageText = message.text.trim(),
            receivedAtEpochMs = message.receivedAtEpochMs,
            proximityMs = DB_DEDUP_PROXIMITY_MS,
        )
        if (existing != null) {
            return@withLock Outcome(
                record = existing,
                shouldAlert = false,
                wasDuplicate = true,
            )
        }

        // Include sender so chat-title brand spoofing (e.g. "Kaspi" on WhatsApp) is scored.
        val evaluationText = buildEvaluationText(message.sender, message.text)
        val rawEvaluation = ruleEngine.evaluate(
            text = evaluationText,
            language = settings.language,
            sensitivity = settings.sensitivity,
        )
        val evaluation = OfficialSenderPolicy.maybeDampen(
            result = rawEvaluation,
            sourceApp = message.sourceApp,
            sender = message.sender,
            bodyText = message.text,
            language = settings.language,
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

        Outcome(
            record = stored,
            shouldAlert = shouldAlert,
            wasDuplicate = false,
        )
    }

    companion object {
        /**
         * Same idea as NotificationCaptureConfig.DB_DEDUP_PROXIMITY_MS — kept here so
         * domain does not depend on the notification package.
         */
        internal const val DB_DEDUP_PROXIMITY_MS: Long = 5 * 60_000L

        internal fun buildEvaluationText(sender: String?, body: String): String {
            val s = sender?.trim().orEmpty()
            return if (s.isEmpty()) body else "$s\n$body"
        }
    }
}
