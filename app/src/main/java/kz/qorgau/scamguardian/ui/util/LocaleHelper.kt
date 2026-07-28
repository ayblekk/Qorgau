package kz.qorgau.scamguardian.ui.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kz.qorgau.scamguardian.domain.model.AppLanguage

object LocaleHelper {
    fun applyLanguage(language: AppLanguage) {
        val tags = language.storageValue
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }
}
