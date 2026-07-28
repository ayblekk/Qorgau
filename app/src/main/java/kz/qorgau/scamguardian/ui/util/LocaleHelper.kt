package kz.qorgau.scamguardian.ui.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kz.qorgau.scamguardian.domain.model.AppLanguage
import java.util.Locale

/**
 * Per-app UI locale via AppCompat. Never call [AppCompatDelegate.setApplicationLocales]
 * unless the tag actually changed — that API recreates activities and can look like a crash loop.
 */
object LocaleHelper {

    private const val PREFS = "scamguardian_locale"
    private const val KEY_TAG = "language_tag"

    /** Persist + apply only when different from the active app locale. */
    fun applyLanguage(language: AppLanguage, context: Context? = null) {
        context?.applicationContext?.let { persistTag(it, language.storageValue) }
        setApplicationLocalesIfNeeded(language.storageValue)
    }

    /**
     * Early apply from SharedPreferences. Safe to call from Application.onCreate
     * **after** super.onCreate().
     */
    fun applyStoredLanguage(context: Context) {
        val tag = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, null)
            ?: return
        setApplicationLocalesIfNeeded(tag)
    }

    /** Keep AppCompat locale aligned with Room settings. */
    fun syncFromSettings(language: AppLanguage, context: Context) {
        persistTag(context.applicationContext, language.storageValue)
        setApplicationLocalesIfNeeded(language.storageValue)
    }

    fun currentLanguage(): AppLanguage {
        val primary = primaryTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())
        return if (primary.isEmpty()) {
            AppLanguage.RUSSIAN
        } else {
            AppLanguage.fromStorage(primary)
        }
    }

    /**
     * Context that resolves strings in the active app language
     * (needed for Application context / notifications).
     */
    fun localizedContext(base: Context): Context {
        val tag = primaryTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())
            .ifEmpty {
                base.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_TAG, AppLanguage.RUSSIAN.storageValue)
                    ?: AppLanguage.RUSSIAN.storageValue
            }
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    private fun setApplicationLocalesIfNeeded(tag: String) {
        val current = primaryTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())
        if (current.equals(tag, ignoreCase = true)) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    private fun primaryTag(languageTags: String): String =
        languageTags.split(',').firstOrNull()?.trim().orEmpty()

    private fun persistTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, tag)
            .apply()
    }
}
