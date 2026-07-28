package kz.qorgau.scamguardian.di

import android.content.Context
import kz.qorgau.scamguardian.data.local.db.ScamGuardianDatabase
import kz.qorgau.scamguardian.data.repository.AnalysisRepositoryImpl
import kz.qorgau.scamguardian.data.repository.SettingsRepositoryImpl
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository

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
}
