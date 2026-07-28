package kz.qorgau.scamguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ScamGuardianApp).container

        lifecycleScope.launch {
            val language = withContext(Dispatchers.IO) {
                container.settingsRepository.getSettings().language
            }
            LocaleHelper.applyLanguage(language)
        }

        setContent {
            ScamGuardianTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScamGuardianApp(
                        viewModelFactory = AppViewModelFactory(container),
                    )
                }
            }
        }
    }
}
