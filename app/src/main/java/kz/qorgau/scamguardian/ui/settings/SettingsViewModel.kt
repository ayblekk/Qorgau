package kz.qorgau.scamguardian.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.domain.capability.DeviceCapability
import kz.qorgau.scamguardian.domain.classifier.ScamClassifier
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.Sensitivity
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository
import kz.qorgau.scamguardian.domain.repository.SettingsRepository
import kz.qorgau.scamguardian.ui.util.LocaleHelper

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val analysisRepository: AnalysisRepository,
    private val readCapability: () -> DeviceCapability,
    private val scamClassifier: ScamClassifier,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository
        .observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    private val _capability = MutableStateFlow(readCapability())
    val capability: StateFlow<DeviceCapability> = _capability.asStateFlow()

    private val _modelAvailable = MutableStateFlow(scamClassifier.isAvailable)
    val modelAvailable: StateFlow<Boolean> = _modelAvailable.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun refreshCapability() {
        _capability.value = readCapability()
        _modelAvailable.value = scamClassifier.isAvailable
    }

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
