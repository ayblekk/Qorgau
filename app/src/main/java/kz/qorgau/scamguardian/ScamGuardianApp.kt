package kz.qorgau.scamguardian

import android.app.Application
import kz.qorgau.scamguardian.di.AppContainer

class ScamGuardianApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
