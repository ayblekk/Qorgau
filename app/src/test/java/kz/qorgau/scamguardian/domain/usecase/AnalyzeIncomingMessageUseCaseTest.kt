package kz.qorgau.scamguardian.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.Sensitivity
import kz.qorgau.scamguardian.domain.model.SourceApp
import kz.qorgau.scamguardian.domain.model.UserFeedback
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.domain.rules.RuleEvaluationResult
import kz.qorgau.scamguardian.domain.rules.RuleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeIncomingMessageUseCaseTest {

    @Test
    fun `skips when monitoring disabled for source`() = runBlocking {
        val useCase = useCase(
            settings = AppSettings(monitorWhatsapp = false),
            risk = RiskLevel.HIGH,
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
        val useCase = useCase(
            settings = AppSettings(),
            risk = RiskLevel.HIGH,
            repo = repo,
        )
        val outcome = useCase.execute(
            IncomingMessage(
                sourceApp = SourceApp.SMS,
                packageName = "com.android.mms",
                sender = "Unknown",
                text = "code please",
                receivedAtEpochMs = 42L,
            ),
        )
        assertNotNull(outcome)
        assertTrue(outcome!!.shouldAlert)
        assertEquals(RiskLevel.HIGH, outcome.record.riskLevel)
        assertEquals(1, repo.stored.size)
    }

    @Test
    fun `suspicious alerts when sensitivity is not low`() = runBlocking {
        val useCase = useCase(
            settings = AppSettings(sensitivity = Sensitivity.MEDIUM),
            risk = RiskLevel.SUSPICIOUS,
        )
        val outcome = useCase.executeManual(sampleMessage())
        assertTrue(outcome.shouldAlert)
        assertEquals(RiskLevel.SUSPICIOUS, outcome.record.riskLevel)
    }

    @Test
    fun `suspicious does not alert when sensitivity is low`() = runBlocking {
        val useCase = useCase(
            settings = AppSettings(sensitivity = Sensitivity.LOW),
            risk = RiskLevel.SUSPICIOUS,
        )
        val outcome = useCase.executeManual(sampleMessage())
        assertFalse(outcome.shouldAlert)
        assertEquals(RiskLevel.SUSPICIOUS, outcome.record.riskLevel)
    }

    @Test
    fun `skips sms when monitor sms disabled`() = runBlocking {
        val useCase = useCase(
            settings = AppSettings(monitorSms = false),
            risk = RiskLevel.HIGH,
        )
        val outcome = useCase.execute(
            IncomingMessage(
                sourceApp = SourceApp.SMS,
                packageName = "com.android.mms",
                sender = "X",
                text = "scam",
                receivedAtEpochMs = 1L,
            ),
        )
        assertNull(outcome)
    }

    @Test
    fun `passes language and sensitivity into stored explanation path`() = runBlocking {
        val useCase = useCase(
            settings = AppSettings(
                language = AppLanguage.KAZAKH,
                sensitivity = Sensitivity.HIGH,
            ),
            risk = RiskLevel.HIGH,
        )
        val outcome = useCase.executeManual(sampleMessage())
        assertEquals(RiskLevel.HIGH, outcome.record.riskLevel)
        assertTrue(outcome.shouldAlert)
    }

    @Test
    fun `buildEvaluationText prepends sender`() {
        assertEquals(
            "Keruen\nПройдите короткий опрос",
            AnalyzeIncomingMessageUseCase.buildEvaluationText(
                "Keruen",
                "Пройдите короткий опрос",
            ),
        )
        assertEquals(
            "only body",
            AnalyzeIncomingMessageUseCase.buildEvaluationText(null, "only body"),
        )
    }

    @Test
    fun `official kaspi sms without override is dampened to safe`() = runBlocking {
        val repo = FakeAnalysisRepository()
        val useCase = AnalyzeIncomingMessageUseCase(
            ruleEngine = FixedResultEngine(
                RuleEvaluationResult(
                    riskLevel = RiskLevel.HIGH,
                    matchedRuleIds = listOf("bank_kaspi_impersonation"),
                    explanation = "bank",
                    confidence = 0.9f,
                    isUncertain = false,
                ),
            ),
            analysisRepository = repo,
            settingsRepository = FakeSettingsRepository(AppSettings()),
        )
        val outcome = useCase.execute(
            IncomingMessage(
                sourceApp = SourceApp.SMS,
                packageName = "com.android.mms",
                sender = "kaspi.kz",
                text = "Kod: 111222",
                receivedAtEpochMs = 1L,
            ),
        )
        assertNotNull(outcome)
        assertEquals(RiskLevel.SAFE, outcome!!.record.riskLevel)
        assertFalse(outcome.shouldAlert)
    }

    @Test
    fun `official kaspi sms with code request is not dampened`() = runBlocking {
        val useCase = AnalyzeIncomingMessageUseCase(
            ruleEngine = FixedResultEngine(
                RuleEvaluationResult(
                    riskLevel = RiskLevel.HIGH,
                    matchedRuleIds = listOf("otp_code_request"),
                    explanation = "otp",
                    confidence = 0.9f,
                    isUncertain = false,
                ),
            ),
            analysisRepository = FakeAnalysisRepository(),
            settingsRepository = FakeSettingsRepository(AppSettings()),
        )
        val outcome = useCase.execute(
            IncomingMessage(
                sourceApp = SourceApp.SMS,
                packageName = "com.android.mms",
                sender = "KaspiBank",
                text = "Пришлите код из СМС",
                receivedAtEpochMs = 1L,
            ),
        )
        assertNotNull(outcome)
        assertEquals(RiskLevel.HIGH, outcome!!.record.riskLevel)
        assertTrue(outcome.shouldAlert)
    }

    private fun sampleMessage() = IncomingMessage(
        sourceApp = SourceApp.MANUAL,
        packageName = "manual",
        sender = null,
        text = "maybe scam",
        receivedAtEpochMs = 1L,
    )

    private fun useCase(
        settings: AppSettings,
        risk: RiskLevel,
        uncertain: Boolean = false,
        repo: FakeAnalysisRepository = FakeAnalysisRepository(),
    ) = AnalyzeIncomingMessageUseCase(
        ruleEngine = FakeRuleEngine(risk, uncertain),
        analysisRepository = repo,
        settingsRepository = FakeSettingsRepository(settings),
    )

    private class FakeRuleEngine(
        private val level: RiskLevel,
        private val uncertain: Boolean = false,
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
                confidence = 0.6f,
                isUncertain = uncertain,
            )
    }

    private class FixedResultEngine(
        private val result: RuleEvaluationResult,
    ) : RuleEngine {
        override fun evaluate(
            text: String,
            language: AppLanguage,
            sensitivity: Sensitivity,
        ): RuleEvaluationResult = result
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
