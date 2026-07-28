package kz.qorgau.scamguardian

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kz.qorgau.scamguardian.di.AppContainer

class ScamGuardianApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        appScope.launch {
            runCatching {
                container.capabilityBootstrap.applyDefaultsIfNeeded()
                container.pruneHistory.execute()
            }
        }
    }
}
