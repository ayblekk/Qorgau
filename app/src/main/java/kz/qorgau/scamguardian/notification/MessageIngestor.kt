package kz.qorgau.scamguardian.notification

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.BuildConfig
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
        if (!deduper.shouldProcess(message)) {
            if (BuildConfig.DEBUG) {
                Log.i(
                    TAG,
                    "Dedup skip source=${message.sourceApp.storageValue} pkg=${message.packageName}",
                )
            }
            return
        }

        scope.launch {
            runCatching {
                val outcome = analyzeIncomingMessage.execute(message)
                if (outcome == null) {
                    if (BuildConfig.DEBUG) {
                        Log.i(
                            TAG,
                            "Monitor off for source=${message.sourceApp.storageValue}",
                        )
                    }
                    return@runCatching
                }
                if (outcome.wasDuplicate) {
                    if (BuildConfig.DEBUG) {
                        Log.i(
                            TAG,
                            "Duplicate skip id=${outcome.record.id} " +
                                "source=${outcome.record.sourceApp.storageValue}",
                        )
                    }
                    return@runCatching
                }
                if (BuildConfig.DEBUG) {
                    Log.i(
                        TAG,
                        "Stored id=${outcome.record.id} risk=${outcome.record.riskLevel.storageValue} " +
                            "source=${outcome.record.sourceApp.storageValue} alert=${outcome.shouldAlert}",
                    )
                }
                if (outcome.shouldAlert) {
                    alertNotifier.showScamAlert(outcome.record)
                }
            }.onFailure { error ->
                // Never log message content (RULES.md §9 / §10).
                Log.e(
                    TAG,
                    "Analysis failed: ${error.javaClass.simpleName}: ${error.message}",
                )
            }
        }
    }

    companion object {
        private const val TAG = "MessageIngestor"
    }
}
