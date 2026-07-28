package kz.qorgau.scamguardian.notification

import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kz.qorgau.scamguardian.ScamGuardianApp

/**
 * Thin capture layer for SMS / WhatsApp / Telegram notifications.
 * Rebinds itself if the system disconnects the listener (common on aggressive OEMs).
 */
class ScamNotificationListenerService : NotificationListenerService() {

    private val extractor = NotificationTextExtractor
    private var ingestor: MessageIngestor? = null

    override fun onCreate() {
        super.onCreate()
        bindIngestor()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        bindIngestor()
        Log.i(TAG, "Notification listener connected")
        // Catch up on active messaging notifications already on the shade.
        runCatching {
            activeNotifications?.forEach { sbn ->
                process(sbn)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected — requesting rebind")
        val component = ComponentName(this, ScamNotificationListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(component)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        process(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op for Stage 1.
    }

    override fun onDestroy() {
        ingestor = null
        super.onDestroy()
    }

    private fun bindIngestor() {
        val app = application as? ScamGuardianApp
        ingestor = app?.container?.messageIngestor
        app?.container?.alertNotifier?.ensureChannel()
    }

    private fun process(sbn: StatusBarNotification) {
        val activeIngestor = ingestor ?: run {
            bindIngestor()
            ingestor
        } ?: return

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

    companion object {
        private const val TAG = "ScamNotifListener"
        private const val DEBUG_LOG = false
    }
}
