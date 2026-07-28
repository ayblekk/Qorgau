package kz.qorgau.scamguardian.notification

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.usecase.AnalyzeIncomingMessageUseCase

/**
 * Async bridge from NotificationListener → domain analysis.
 * Never blocks the binder thread of the listener service.
 */
class MessageIngestor(
    private val analyzeIncomingMessage: AnalyzeIncomingMessageUseCase,
    private val alertNotifier: AlertNotifier,
    private val deduper: NotificationDeduper = NotificationDeduper(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    fun ingestFromNotification(message: IncomingMessage) {
        if (!deduper.shouldProcess(message)) return

        scope.launch {
            runCatching {
                val outcome = analyzeIncomingMessage.execute(message) ?: return@runCatching
                if (outcome.shouldAlert) {
                    alertNotifier.showScamAlert(outcome.record)
                }
            }.onFailure { error ->
                // Never log message content (RULES.md §10).
                Log.w(TAG, "Analysis failed: ${error.javaClass.simpleName}")
            }
        }
    }

    companion object {
        private const val TAG = "MessageIngestor"
    }
}
