package kz.qorgau.scamguardian.notification

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log
import kz.qorgau.scamguardian.ui.util.isNotificationListenerEnabled

/**
 * Tracks NLS bind state and forces rebind after OEM kills / APK updates.
 *
 * System often keeps the toggle "ON" while the service is disconnected — that
 * is the usual reason History stays empty while WhatsApp/SMS still fire.
 */
object NotificationListenerController {

    @Volatile
    var isConnected: Boolean = false
        private set

    @Volatile
    var lastConnectedAtMs: Long = 0L
        private set

    @Volatile
    var lastDisconnectedAtMs: Long = 0L
        private set

    @Volatile
    var lastCaptureAtMs: Long = 0L
        private set

    @Volatile
    var lastCaptureSummary: String? = null
        private set

    @Volatile
    private var lastEnsureAtMs: Long = 0L

    @Volatile
    private var lastBounceAtMs: Long = 0L

    fun markConnected() {
        isConnected = true
        lastConnectedAtMs = System.currentTimeMillis()
        Log.i(TAG, "Listener connected")
    }

    fun markDisconnected() {
        isConnected = false
        lastDisconnectedAtMs = System.currentTimeMillis()
        Log.w(TAG, "Listener disconnected")
    }

    fun markCapture(source: String, packageName: String, textLength: Int) {
        lastCaptureAtMs = System.currentTimeMillis()
        lastCaptureSummary = "$source|$packageName|len=$textLength"
    }

    /**
     * Call on app foreground / boot / package replace.
     * Safe to call often — throttled rebind + rare component bounce.
     */
    fun ensureBound(context: Context, forceBounce: Boolean = false) {
        val appContext = context.applicationContext
        if (!isNotificationListenerEnabled(appContext)) {
            Log.i(TAG, "ensureBound: notification access not granted")
            return
        }

        val now = System.currentTimeMillis()
        if (!forceBounce && now - lastEnsureAtMs < ENSURE_THROTTLE_MS && isConnected) {
            return
        }
        lastEnsureAtMs = now

        val component = ComponentName(appContext, ScamNotificationListenerService::class.java)
        requestRebind(component)

        // Toggle "enabled" but binder dead → bounce the component once in a while.
        val stuck = !isConnected &&
            (lastConnectedAtMs == 0L || now - lastConnectedAtMs > STUCK_AFTER_MS)
        if (forceBounce || stuck) {
            if (forceBounce || now - lastBounceAtMs > BOUNCE_THROTTLE_MS) {
                lastBounceAtMs = now
                bounceComponent(appContext, component)
                requestRebind(component)
                Log.i(TAG, "ensureBound: component bounce + rebind (force=$forceBounce)")
            }
        } else {
            Log.i(TAG, "ensureBound: rebind requested connected=$isConnected")
        }
    }

    private fun requestRebind(component: ComponentName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        runCatching {
            NotificationListenerService.requestRebind(component)
        }.onFailure {
            Log.w(TAG, "requestRebind failed: ${it.javaClass.simpleName}")
        }
    }

    /**
     * Disable → enable the service component so the system re-binds it.
     * Does not revoke user permission; only restarts the listener binding.
     */
    private fun bounceComponent(context: Context, component: ComponentName) {
        val pm = context.packageManager
        runCatching {
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }.onFailure {
            Log.w(TAG, "component bounce failed: ${it.javaClass.simpleName}")
        }
    }

    private const val TAG = "NlsController"
    private const val ENSURE_THROTTLE_MS = 5_000L
    private const val BOUNCE_THROTTLE_MS = 30_000L
    private const val STUCK_AFTER_MS = 15_000L
}
