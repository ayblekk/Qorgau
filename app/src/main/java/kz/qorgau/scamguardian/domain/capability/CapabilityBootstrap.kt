package kz.qorgau.scamguardian.domain.capability

import android.content.Context
import kz.qorgau.scamguardian.domain.repository.SettingsRepository

/**
 * Applies recommended analysis defaults once on first launch.
 * User can override later in Settings (ARCHITECTURE.md §3.5).
 */
class CapabilityBootstrap(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val readCapability: () -> DeviceCapability,
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    suspend fun applyDefaultsIfNeeded(): DeviceCapability {
        val capability = readCapability()
        if (prefs.getBoolean(KEY_APPLIED, false)) {
            return capability
        }
        val current = settingsRepository.getSettings()
        settingsRepository.updateSettings(
            current.copy(
                rulesOnlyMode = capability.recommendRulesOnly,
                modelEnabled = capability.recommendModelEnabled,
            ),
        )
        prefs.edit().putBoolean(KEY_APPLIED, true).apply()
        return capability
    }

    companion object {
        private const val PREFS_NAME = "scamguardian_capability"
        private const val KEY_APPLIED = "defaults_applied_v1"
    }
}
