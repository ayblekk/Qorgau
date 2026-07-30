package kz.qorgau.scamguardian.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kz.qorgau.scamguardian.MainActivity
import kz.qorgau.scamguardian.R
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.ui.util.LocaleHelper

/**
 * Shows a high-priority local alert. Content never leaves the device.
 *
 * Privacy: lock-screen / public shade must NOT show message body (RULES.md §1, §9).
 * Full text lives only inside the app History screen.
 * Tone is intentionally urgent so the user stops before acting on a scam.
 * Strings follow the user's selected app language (RU / KK / EN).
 */
class AlertNotifier(
    context: Context,
) {
    private val appContext = context.applicationContext

    private fun strings(): Context = LocaleHelper.localizedContext(appContext)

    fun ensureChannel() {
        val res = strings()
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            res.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = res.getString(R.string.alert_channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
            enableLights(true)
            lightColor = Color.RED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowBubbles(false)
            }
            // Hide sensitive content on secure lock screen.
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }

    fun showScamAlert(record: AnalysisRecord) {
        ensureChannel()
        val res = strings()

        val openApp = PendingIntent.getActivity(
            appContext,
            // Stable request code without truncating large ids into collisions.
            (record.id % Int.MAX_VALUE).toInt().let { if (it == 0) 1 else it },
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ANALYSIS_ID, record.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when (record.riskLevel) {
            RiskLevel.HIGH -> res.getString(R.string.alert_title_high)
            RiskLevel.SUSPICIOUS -> res.getString(R.string.alert_title_suspicious)
            RiskLevel.SAFE -> return
        }

        val shortBody = when (record.riskLevel) {
            RiskLevel.HIGH -> res.getString(R.string.alert_short_high)
            RiskLevel.SUSPICIOUS -> res.getString(R.string.alert_short_suspicious)
            RiskLevel.SAFE -> return
        }

        // Explanation only — never embed raw message text in the system shade.
        val bigBody = when (record.riskLevel) {
            RiskLevel.HIGH -> res.getString(
                R.string.alert_body_high_private,
                record.explanation,
            )
            RiskLevel.SUSPICIOUS -> res.getString(
                R.string.alert_body_suspicious_private,
                record.explanation,
            )
            RiskLevel.SAFE -> return
        }

        val publicLockscreen = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(bigBody),
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicLockscreen)
            .setColor(Color.parseColor("#D32F2F"))
            .setColorized(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setOnlyAlertOnce(false)
            .build()

        // May require POST_NOTIFICATIONS on API 33+ — request is handled in onboarding.
        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(NOTIFICATION_TAG, (record.id % Int.MAX_VALUE).toInt(), notification)
        }
    }

    companion object {
        /** Bumped channel id so PRIVATE lock-screen visibility applies on upgrade. */
        const val CHANNEL_ID: String = "scam_alerts_v3"
        const val NOTIFICATION_TAG: String = "scamguardian_alert"
        const val EXTRA_ANALYSIS_ID: String = "extra_analysis_id"
    }
}
