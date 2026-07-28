package kz.qorgau.scamguardian

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.di.AppContainer
import kz.qorgau.scamguardian.ui.util.LocaleHelper

class ScamGuardianApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // After super: AppCompat is ready; restore UI language if we saved one.
        LocaleHelper.applyStoredLanguage(this)
        container = AppContainer(this)

        appScope.launch {
            runCatching {
                val language = container.settingsRepository.getSettings().language
                // Only updates if different — avoids Activity recreate loop on cold start.
                LocaleHelper.syncFromSettings(language, this@ScamGuardianApp)
                container.pruneHistory.execute()
            }
        }
    }
}
