package kz.qorgau.scamguardian.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.Sensitivity
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.ui.util.LocaleHelper

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val analysisRepository: AnalysisRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository
        .observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings()
            settingsRepository.updateSettings(current.copy(language = language))
            LocaleHelper.applyLanguage(language)
        }
    }

    fun setSensitivity(sensitivity: Sensitivity) {
        update { it.copy(sensitivity = sensitivity) }
    }

    fun setRulesOnly(enabled: Boolean) {
        update { it.copy(rulesOnlyMode = enabled) }
    }

    fun setModelEnabled(enabled: Boolean) {
        update { it.copy(modelEnabled = enabled) }
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
            val current = settingsRepository.getSettings()
            settingsRepository.updateSettings(transform(current))
        }
    }
}

sealed interface SettingsEvent {
    data object HistoryCleared : SettingsEvent
}
