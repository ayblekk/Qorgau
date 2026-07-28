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
                // Always log drop reasons for messaging-ish packages to diagnose OEM gaps.
                val pkg = sbn.packageName.orEmpty()
                if (DEBUG_LOG || isInterestingPackage(pkg)) {
                    Log.i(
                        TAG,
                        "Ignored: ${result.reason} pkg=$pkg cat=${sbn.notification?.category}",
                    )
                }
            }
            is NotificationTextExtractor.ExtractResult.Success -> {
                val msg = result.message
                Log.i(
                    TAG,
                    "Captured source=${msg.sourceApp.storageValue} pkg=${msg.packageName} " +
                        "len=${msg.text.length}",
                )
                activeIngestor.ingestFromNotification(msg)
            }
        }
    }

    private fun isInterestingPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("whatsapp") ||
            lower.contains("telegram") ||
            lower.contains("mms") ||
            lower.contains("messag") ||
            lower.contains("sms") ||
            lower.contains("signal") ||
            lower.contains("viber")
    }

    companion object {
        private const val TAG = "ScamNotifListener"
        private const val DEBUG_LOG = false
    }
}
