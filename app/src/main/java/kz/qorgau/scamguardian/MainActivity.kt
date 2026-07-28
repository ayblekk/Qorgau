package kz.qorgau.scamguardian

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.qorgau.scamguardian.ui.AppViewModelFactory
import kz.qorgau.scamguardian.ui.navigation.ScamGuardianApp
import kz.qorgau.scamguardian.ui.theme.ScamGuardianTheme
import kz.qorgau.scamguardian.ui.util.LocaleHelper

/**
 * Entry activity with bottom navigation (DESIGN.md).
 * Extends AppCompatActivity so per-app locales (RU/KK/EN) apply correctly.
 * Theme must be Theme.AppCompat* (see values/themes.xml).
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ScamGuardianApp
        val container = app.container

        setContent {
            ScamGuardianTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScamGuardianApp(
                        viewModelFactory = AppViewModelFactory(app, container),
                    )
                }
            }
        }

        // Align stored locale with Room after first frame (no-op if already matching).
        lifecycleScope.launch {
            val language = withContext(Dispatchers.IO) {
                container.settingsRepository.getSettings().language
            }
            LocaleHelper.syncFromSettings(language, this@MainActivity)
        }
    }
}
