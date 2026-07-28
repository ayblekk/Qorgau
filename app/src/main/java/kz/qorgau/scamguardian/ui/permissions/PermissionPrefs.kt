package kz.qorgau.scamguardian.ui.permissions

import android.content.Context

/**
 * Tracks whether the user completed / dismissed the first-run permission wizard.
 */
class PermissionPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS,
        Context.MODE_PRIVATE,
    )

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    companion object {
        private const val PREFS = "scamguardian_permissions"
        private const val KEY_ONBOARDING_DONE = "onboarding_completed_v1"
    }
}
