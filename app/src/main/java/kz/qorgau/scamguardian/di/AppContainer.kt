package kz.qorgau.scamguardian.di

import android.content.Context
import kz.qorgau.scamguardian.data.capability.AndroidDeviceCapabilityReader
import kz.qorgau.scamguardian.data.local.db.ScamGuardianDatabase
import kz.qorgau.scamguardian.data.repository.AnalysisRepositoryImpl
import kz.qorgau.scamguardian.data.repository.SettingsRepositoryImpl
import kz.qorgau.scamguardian.data.rules.RulePackLoader
import kz.qorgau.scamguardian.domain.capability.CapabilityBootstrap
import kz.qorgau.scamguardian.domain.capability.DeviceCapability
import kz.qorgau.scamguardian.domain.classifier.ScamClassifier
import kz.qorgau.scamguardian.domain.classifier.TimedScamClassifier
import kz.qorgau.scamguardian.domain.classifier.UnavailableScamClassifier
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.domain.rules.DefaultRuleEngine
import kz.qorgau.scamguardian.domain.rules.RuleEngine
import kz.qorgau.scamguardian.domain.usecase.AnalyzeIncomingMessageUseCase
import kz.qorgau.scamguardian.domain.usecase.PruneHistoryUseCase
import kz.qorgau.scamguardian.notification.AlertNotifier
import kz.qorgau.scamguardian.notification.MessageIngestor
import kz.qorgau.scamguardian.notification.NotificationDeduper

/**
 * Manual composition root (KISS — no Hilt for Stage 1).
 * UI and services obtain dependencies only through this container.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: ScamGuardianDatabase by lazy {
        ScamGuardianDatabase.getInstance(appContext)
    }

    val analysisRepository: AnalysisRepository by lazy {
        AnalysisRepositoryImpl(database.analysisLogDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(database.appSettingsDao())
    }

    val rulePackLoader: RulePackLoader by lazy { RulePackLoader() }

    val ruleEngine: RuleEngine by lazy {
        val pack = rulePackLoader.loadFromAssets(appContext)
        DefaultRuleEngine(pack)
    }

    val deviceCapabilityReader: AndroidDeviceCapabilityReader by lazy {
        AndroidDeviceCapabilityReader(appContext)
    }

    val capabilityBootstrap: CapabilityBootstrap by lazy {
        CapabilityBootstrap(
            context = appContext,
            settingsRepository = settingsRepository,
            readCapability = { deviceCapabilityReader.read() },
        )
    }

    /**
     * Stage 1 ships without model weights — always unavailable.
     * Swap [UnavailableScamClassifier] for a real runtime later without changing the pipeline.
     */
    val scamClassifier: ScamClassifier by lazy {
        TimedScamClassifier(UnavailableScamClassifier())
    }

    val analyzeIncomingMessage: AnalyzeIncomingMessageUseCase by lazy {
        AnalyzeIncomingMessageUseCase(
            ruleEngine = ruleEngine,
            analysisRepository = analysisRepository,
            settingsRepository = settingsRepository,
            scamClassifier = scamClassifier,
        )
    }

    val pruneHistory: PruneHistoryUseCase by lazy {
        PruneHistoryUseCase(analysisRepository)
    }

    val alertNotifier: AlertNotifier by lazy {
        AlertNotifier(appContext)
    }

    val messageIngestor: MessageIngestor by lazy {
        MessageIngestor(
            analyzeIncomingMessage = analyzeIncomingMessage,
            alertNotifier = alertNotifier,
            deduper = NotificationDeduper(),
        )
    }

    fun currentCapability(): DeviceCapability = deviceCapabilityReader.read()
}
