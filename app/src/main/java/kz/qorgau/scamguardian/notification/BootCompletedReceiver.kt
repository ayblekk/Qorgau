package kz.qorgau.scamguardian.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * After reboot / update, force the notification listener to rebind.
 * Package replace is the usual moment NLS sticks in "enabled but dead".
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        // After APK update, bounce the component so OEM re-attaches the binder.
        val forceBounce = action == Intent.ACTION_MY_PACKAGE_REPLACED
        NotificationListenerController.ensureBound(context, forceBounce = forceBounce)
        Log.i(TAG, "ensureBound after $action forceBounce=$forceBounce")
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
