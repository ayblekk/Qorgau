package kz.qorgau.scamguardian.ui.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.SourceApp
import kz.qorgau.scamguardian.domain.usecase.AnalyzeIncomingMessageUseCase

data class CheckUiState(
    val input: String = "",
    val isAnalyzing: Boolean = false,
    val result: AnalysisRecord? = null,
    val errorMessageRes: Int? = null,
)

class CheckViewModel(
    private val analyzeIncomingMessage: AnalyzeIncomingMessageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckUiState())
    val uiState: StateFlow<CheckUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.update {
            it.copy(input = value, errorMessageRes = null)
        }
    }

    fun clear() {
        _uiState.value = CheckUiState()
    }

    fun analyze() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty()) {
            _uiState.update {
                it.copy(errorMessageRes = kz.qorgau.scamguardian.R.string.check_empty_error)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isAnalyzing = true, errorMessageRes = null, result = null)
            }
            val outcome = analyzeIncomingMessage.executeManual(
                IncomingMessage(
                    sourceApp = SourceApp.MANUAL,
                    packageName = "manual",
                    sender = null,
                    text = text,
                    receivedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            _uiState.update {
                it.copy(isAnalyzing = false, result = outcome.record)
            }
        }
    }
}
