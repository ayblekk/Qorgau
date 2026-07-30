package kz.qorgau.scamguardian.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kz.qorgau.scamguardian.BuildConfig
import kz.qorgau.scamguardian.ScamGuardianApp

/**
 * Thin capture layer for SMS / WhatsApp / Telegram / other messengers.
 * Rebinds itself if the system disconnects the listener (common on aggressive OEMs).
 */
class ScamNotificationListenerService : NotificationListenerService() {

    private val extractor = NotificationTextExtractor
    private var ingestor: MessageIngestor? = null

    override fun onCreate() {
        super.onCreate()
        bindIngestor()
        Log.i(TAG, "Service onCreate")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        bindIngestor()
        NotificationListenerController.markConnected()
        Log.i(TAG, "Notification listener connected")
        // Catch up on active messaging notifications already on the shade.
        reprocessActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationListenerController.markDisconnected()
        Log.w(TAG, "Notification listener disconnected — requesting rebind")
        // requestRebind from the service context after disconnect.
        NotificationListenerController.ensureBound(this, forceBounce = false)
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

    private fun reprocessActiveNotifications() {
        runCatching {
            val active = activeNotifications
            Log.i(TAG, "Reprocessing activeNotifications count=${active?.size ?: 0}")
            active?.forEach { sbn -> process(sbn) }
        }.onFailure {
            Log.w(TAG, "activeNotifications failed: ${it.javaClass.simpleName}")
        }
    }

    private fun bindIngestor() {
        val app = application as? ScamGuardianApp
        if (app == null) {
            Log.e(TAG, "Application is not ScamGuardianApp — cannot bind ingestor")
            ingestor = null
            return
        }
        ingestor = app.container.messageIngestor
        app.container.alertNotifier.ensureChannel()
    }

    private fun process(sbn: StatusBarNotification) {
        val activeIngestor = ingestor ?: run {
            bindIngestor()
            ingestor
        }
        if (activeIngestor == null) {
            Log.e(
                TAG,
                "Drop: no MessageIngestor pkg=${sbn.packageName}",
            )
            return
        }

        when (val result = extractor.extract(sbn, packageName)) {
            is NotificationTextExtractor.ExtractResult.Ignored -> {
                // Debug only — never log message body (RULES.md §9).
                if (BuildConfig.DEBUG) {
                    val pkg = sbn.packageName.orEmpty()
                    if (isInterestingPackage(pkg)) {
                        Log.i(
                            TAG,
                            "Ignored: ${result.reason} pkg=$pkg cat=${sbn.notification?.category}",
                        )
                    }
                }
            }
            is NotificationTextExtractor.ExtractResult.Success -> {
                val msg = result.message
                NotificationListenerController.markCapture(
                    source = msg.sourceApp.storageValue,
                    packageName = msg.packageName,
                    textLength = msg.text.length,
                )
                if (BuildConfig.DEBUG) {
                    Log.i(
                        TAG,
                        "Captured source=${msg.sourceApp.storageValue} pkg=${msg.packageName} " +
                            "len=${msg.text.length}",
                    )
                }
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
            lower.contains("viber") ||
            lower.contains("instagram") ||
            lower.contains("facebook") ||
            lower.contains("vkontakte") ||
            lower.contains("beeline") ||
            lower.contains("telephony")
    }

    companion object {
        private const val TAG = "ScamNotifListener"
    }
}
