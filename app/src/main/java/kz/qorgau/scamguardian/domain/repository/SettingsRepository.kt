package kz.qorgau.scamguardian.domain.repository

import kotlinx.coroutines.flow.Flow
import kz.qorgau.scamguardian.domain.model.AppSettings

/**
 * Single-row settings stored on device (SCHEMA.md §3.2).
 */
interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings)
}
