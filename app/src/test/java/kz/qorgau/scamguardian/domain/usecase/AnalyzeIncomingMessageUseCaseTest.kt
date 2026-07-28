package kz.qorgau.scamguardian.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kz.qorgau.scamguardian.domain.classifier.ClassifierResult
import kz.qorgau.scamguardian.domain.classifier.ScamClassifier
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
                sender = "Kaspi",
                text = "code please",
                receivedAtEpochMs = 42L,
            ),
        )
        assertNotNull(outcome)
        assertTrue(outcome!!.shouldAlert)
        assertEquals(RiskLevel.HIGH, outcome.record.riskLevel)
        assertEquals(1, repo.stored.size)
        assertFalse(outcome.usedClassifier)
    }

    @Test
    fun `does not call classifier when rules only mode`() = runBlocking {
        val classifier = CountingClassifier(isAvailable = true, score = 0.9f)
        val useCase = useCase(
            settings = AppSettings(rulesOnlyMode = true, modelEnabled = true),
            risk = RiskLevel.SUSPICIOUS,
            uncertain = true,
            classifier = classifier,
        )
        val outcome = useCase.executeManual(sampleMessage())
        assertEquals(0, classifier.calls)
        assertFalse(outcome.usedClassifier)
        assertEquals(RiskLevel.SUSPICIOUS, outcome.record.riskLevel)
    }

    @Test
    fun `uses classifier when uncertain and model enabled`() = runBlocking {
        val classifier = CountingClassifier(isAvailable = true, score = 0.9f)
        val useCase = useCase(
            settings = AppSettings(rulesOnlyMode = false, modelEnabled = true),
            risk = RiskLevel.SUSPICIOUS,
            uncertain = true,
            classifier = classifier,
        )
        val outcome = useCase.executeManual(sampleMessage())
        assertEquals(1, classifier.calls)
        assertTrue(outcome.usedClassifier)
        assertEquals(RiskLevel.HIGH, outcome.record.riskLevel)
    }

    @Test
    fun `falls back to rules when classifier unavailable`() = runBlocking {
        val classifier = CountingClassifier(isAvailable = false, score = 0.9f)
        val useCase = useCase(
            settings = AppSettings(modelEnabled = true),
            risk = RiskLevel.SUSPICIOUS,
            uncertain = true,
            classifier = classifier,
        )
        val outcome = useCase.executeManual(sampleMessage())
        assertEquals(0, classifier.calls)
        assertFalse(outcome.usedClassifier)
        assertEquals(RiskLevel.SUSPICIOUS, outcome.record.riskLevel)
    }

    @Test
    fun `falls back to rules when classifier returns null`() = runBlocking {
        val classifier = object : ScamClassifier {
            override val isAvailable: Boolean = true
            override suspend fun classify(text: String): ClassifierResult? = null
        }
        val useCase = useCase(
            settings = AppSettings(modelEnabled = true),
            risk = RiskLevel.SUSPICIOUS,
            uncertain = true,
            classifier = classifier,
        )
        val outcome = useCase.executeManual(sampleMessage())
        assertFalse(outcome.usedClassifier)
        assertEquals(RiskLevel.SUSPICIOUS, outcome.record.riskLevel)
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
        classifier: ScamClassifier = CountingClassifier(isAvailable = false),
    ) = AnalyzeIncomingMessageUseCase(
        ruleEngine = FakeRuleEngine(risk, uncertain),
        analysisRepository = repo,
        settingsRepository = FakeSettingsRepository(settings),
        scamClassifier = classifier,
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

    private class CountingClassifier(
        override val isAvailable: Boolean,
        private val score: Float = 0.5f,
    ) : ScamClassifier {
        var calls: Int = 0
        override suspend fun classify(text: String): ClassifierResult {
            calls++
            return ClassifierResult(riskScore = score, explanation = "from model")
        }
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
