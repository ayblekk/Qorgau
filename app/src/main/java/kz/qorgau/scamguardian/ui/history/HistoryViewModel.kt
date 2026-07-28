package kz.qorgau.scamguardian.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.UserFeedback
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository

class HistoryViewModel(
    private val analysisRepository: AnalysisRepository,
) : ViewModel() {

    val history: StateFlow<List<AnalysisRecord>> = analysisRepository
        .observeHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun markRead(id: Long) {
        viewModelScope.launch {
            analysisRepository.markRead(id)
        }
    }

    fun markFalsePositive(id: Long) {
        viewModelScope.launch {
            analysisRepository.setFeedback(id, UserFeedback.FALSE_POSITIVE)
            analysisRepository.markRead(id)
        }
    }

    fun markConfirmed(id: Long) {
        viewModelScope.launch {
            analysisRepository.setFeedback(id, UserFeedback.CONFIRMED)
            analysisRepository.markRead(id)
        }
    }
}
