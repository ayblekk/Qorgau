package kz.qorgau.scamguardian.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kz.qorgau.scamguardian.ScamGuardianApp

/**
 * Thin capture layer (ARCHITECTURE.md §3.1, RULES.md §3).
 * Extracts text and hands off to [MessageIngestor] — no analysis here.
 */
class ScamNotificationListenerService : NotificationListenerService() {

    private val extractor = NotificationTextExtractor
    private var ingestor: MessageIngestor? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as? ScamGuardianApp
        ingestor = app?.container?.messageIngestor
        app?.container?.alertNotifier?.ensureChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val activeIngestor = ingestor ?: return

        when (val result = extractor.extract(sbn, packageName)) {
            is NotificationTextExtractor.ExtractResult.Ignored -> {
                if (DEBUG_LOG) {
                    Log.d(TAG, "Ignored: ${result.reason} pkg=${sbn.packageName}")
                }
            }
            is NotificationTextExtractor.ExtractResult.Success -> {
                activeIngestor.ingestFromNotification(result.message)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op for Stage 1.
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
    }

    override fun onDestroy() {
        ingestor = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ScamNotifListener"

        /** Debug-only reasons; never logs message bodies. */
        private const val DEBUG_LOG = false
    }
}
