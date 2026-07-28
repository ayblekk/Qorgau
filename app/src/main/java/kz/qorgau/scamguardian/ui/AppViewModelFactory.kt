package kz.qorgau.scamguardian.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kz.qorgau.scamguardian.di.AppContainer
import kz.qorgau.scamguardian.ui.check.CheckViewModel
import kz.qorgau.scamguardian.ui.history.HistoryViewModel
import kz.qorgau.scamguardian.ui.settings.SettingsViewModel

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(container.analysisRepository) as T
            }
            modelClass.isAssignableFrom(CheckViewModel::class.java) -> {
                CheckViewModel(container.analyzeIncomingMessage) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    analysisRepository = container.analysisRepository,
                ) as T
            }
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
