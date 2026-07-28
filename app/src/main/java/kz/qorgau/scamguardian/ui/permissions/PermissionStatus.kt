package kz.qorgau.scamguardian.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kz.qorgau.scamguardian.ui.util.isNotificationListenerEnabled

data class PermissionSnapshot(
    val postNotificationsGranted: Boolean,
    val notificationListenerEnabled: Boolean,
    val batteryOptimizationIgnored: Boolean,
) {
    val allCriticalGranted: Boolean
        get() = postNotificationsGranted && notificationListenerEnabled

    val allGranted: Boolean
        get() = allCriticalGranted && batteryOptimizationIgnored
}

object PermissionStatus {

    fun snapshot(context: Context): PermissionSnapshot =
        PermissionSnapshot(
            postNotificationsGranted = isPostNotificationsGranted(context),
            notificationListenerEnabled = isNotificationListenerEnabled(context),
            batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context),
        )

    fun isPostNotificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun postNotificationsPermission(): String = Manifest.permission.POST_NOTIFICATIONS

    fun batteryOptimizationIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun notificationListenerIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }

    fun canPostNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            isPostNotificationsGranted(context)
}
