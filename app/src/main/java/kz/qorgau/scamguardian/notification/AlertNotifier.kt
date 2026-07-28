package kz.qorgau.scamguardian.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kz.qorgau.scamguardian.MainActivity
import kz.qorgau.scamguardian.R
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.RiskLevel

/**
 * Shows a high-priority local alert. Content never leaves the device.
 */
class AlertNotifier(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun ensureChannel() {
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.alert_channel_description)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showScamAlert(record: AnalysisRecord) {
        ensureChannel()

        val openApp = PendingIntent.getActivity(
            appContext,
            record.id.toInt(),
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ANALYSIS_ID, record.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when (record.riskLevel) {
            RiskLevel.HIGH -> appContext.getString(R.string.alert_title_high)
            RiskLevel.SUSPICIOUS -> appContext.getString(R.string.alert_title_suspicious)
            RiskLevel.SAFE -> return
        }

        val preview = record.messageText
            .replace('\n', ' ')
            .take(120)

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(title)
            .setContentText(record.explanation)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${record.explanation}\n\n$preview"),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .build()

        // May require POST_NOTIFICATIONS on API 33+ — request is handled in Settings UI later.
        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(NOTIFICATION_TAG, record.id.toInt(), notification)
        }
    }

    companion object {
        const val CHANNEL_ID: String = "scam_alerts"
        const val NOTIFICATION_TAG: String = "scamguardian_alert"
        const val EXTRA_ANALYSIS_ID: String = "extra_analysis_id"
    }
}
