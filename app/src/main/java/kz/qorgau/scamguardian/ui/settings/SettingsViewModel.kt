package kz.qorgau.scamguardian.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.Sensitivity
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.ui.util.LocaleHelper

/**
 * Settings are persisted in Room and reflected immediately in UI (optimistic update).
 * Language also drives AppCompat per-app locales (RU / KK / EN resources).
 */
class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val analysisRepository: AnalysisRepository,
) : AndroidViewModel(application) {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            // Seed defaults if the single-row table is empty.
            settingsRepository.getSettings()
            settingsRepository.observeSettings().collect { stored ->
                _settings.value = stored
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            val next = settingsRepository.getSettings().copy(language = language)
            _settings.value = next
            settingsRepository.updateSettings(next)
            // Apply after DB write so recreated Activity loads the new language.
            LocaleHelper.syncFromSettings(language, getApplication())
        }
    }

    fun setSensitivity(sensitivity: Sensitivity) {
        update { it.copy(sensitivity = sensitivity) }
    }

    fun setMonitorSms(enabled: Boolean) {
        update { it.copy(monitorSms = enabled) }
    }

    fun setMonitorWhatsapp(enabled: Boolean) {
        update { it.copy(monitorWhatsapp = enabled) }
    }

    fun setMonitorTelegram(enabled: Boolean) {
        update { it.copy(monitorTelegram = enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            analysisRepository.clearAll()
            _events.emit(SettingsEvent.HistoryCleared)
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val next = transform(settingsRepository.getSettings())
            _settings.value = next
            settingsRepository.updateSettings(next)
        }
    }
}

sealed interface SettingsEvent {
    data object HistoryCleared : SettingsEvent
}
