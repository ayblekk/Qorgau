package kz.qorgau.scamguardian.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.SourceApp
import kz.qorgau.scamguardian.domain.model.UserFeedback
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.domain.rules.RuleEvaluationResult
import kz.qorgau.scamguardian.domain.rules.RuleEngine
import kz.qorgau.scamguardian.domain.model.Sensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeIncomingMessageUseCaseTest {

    @Test
    fun `skips when monitoring disabled for source`() = runBlocking {
        val settings = FakeSettingsRepository(
            AppSettings(monitorWhatsapp = false),
        )
        val useCase = AnalyzeIncomingMessageUseCase(
            ruleEngine = FakeRuleEngine(RiskLevel.HIGH),
            analysisRepository = FakeAnalysisRepository(),
            settingsRepository = settings,
        )
        val outcome = useCase.execute(
            IncomingMessage(
                sourceApp = SourceApp.WHATSAPP,
                packageName = "com.whatsapp",
                sender = "X",
                text = "scam",
                receivedAtEpochMs = 1L,
            ),
        )
        assertNull(outcome)
    }

    @Test
    fun `stores high risk and requests alert`() = runBlocking {
        val repo = FakeAnalysisRepository()
        val useCase = AnalyzeIncomingMessageUseCase(
            ruleEngine = FakeRuleEngine(RiskLevel.HIGH),
            analysisRepository = repo,
            settingsRepository = FakeSettingsRepository(AppSettings()),
        )
        val outcome = useCase.execute(
            IncomingMessage(
                sourceApp = SourceApp.SMS,
                packageName = "com.android.mms",
                sender = "Kaspi",
                text = "code please",
                receivedAtEpochMs = 42L,
            ),
        )
        assertNotNull(outcome)
        assertTrue(outcome!!.shouldAlert)
        assertEquals(RiskLevel.HIGH, outcome.record.riskLevel)
        assertEquals(1, repo.stored.size)
        assertEquals(42L, outcome.record.createdAtEpochMs)
    }

    private class FakeRuleEngine(
        private val level: RiskLevel,
    ) : RuleEngine {
        override fun evaluate(
            text: String,
            language: AppLanguage,
            sensitivity: Sensitivity,
        ): RuleEvaluationResult =
            RuleEvaluationResult(
                riskLevel = level,
                matchedRuleIds = listOf("fake"),
                explanation = "test",
                confidence = 0.9f,
                isUncertain = false,
            )
    }

    private class FakeSettingsRepository(
        initial: AppSettings,
    ) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override fun observeSettings(): Flow<AppSettings> = state
        override suspend fun getSettings(): AppSettings = state.value
        override suspend fun updateSettings(settings: AppSettings) {
            state.value = settings
        }
    }

    private class FakeAnalysisRepository : AnalysisRepository {
        val stored = mutableListOf<AnalysisRecord>()
        private var seq = 0L

        override fun observeHistory(): Flow<List<AnalysisRecord>> =
            MutableStateFlow(stored.toList())

        override fun observeUnreadCount(): Flow<Int> =
            MutableStateFlow(stored.count { !it.isRead })

        override suspend fun getById(id: Long): AnalysisRecord? =
            stored.firstOrNull { it.id == id }

        override suspend fun insert(record: AnalysisRecord): Long {
            val id = ++seq
            stored += record.copy(id = id)
            return id
        }

        override suspend fun markRead(id: Long) = Unit
        override suspend fun setFeedback(id: Long, feedback: UserFeedback) = Unit
        override suspend fun deleteOlderThan(epochMs: Long): Int = 0
        override suspend fun clearAll() = stored.clear()
        override suspend fun findByRiskLevel(level: RiskLevel): List<AnalysisRecord> =
            stored.filter { it.riskLevel == level }
    }
}
