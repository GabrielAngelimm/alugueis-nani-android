package com.rentalvalidator.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.data.local.datastore.AppTheme
import com.rentalvalidator.app.presentation.navigation.NaniApp
import com.rentalvalidator.app.presentation.theme.RentalValidatorTheme
import com.rentalvalidator.app.presentation.viewmodel.SettingsViewModel
import com.rentalvalidator.app.reminders.RentReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var openTenant by mutableStateOf<String?>(null)
    private var openPeriod by mutableStateOf<String?>(null)

    private fun readReminder(intent: Intent) {
        openTenant = intent.getStringExtra(RentReminderScheduler.EXTRA_TENANT)
        openPeriod = intent.getStringExtra(RentReminderScheduler.EXTRA_PERIOD)
    }

    /**
     * Every screen is written in Portuguese, so framework widgets (date picker, menus) follow
     * pt-BR as well instead of mixing in the device language. Number and date formats in the
     * app already use explicit pt-BR locales.
     *
     * Text is also pinned to the designed size: the system font-size setting does not scale it,
     * so every screen keeps the proportions it was drawn with. This is applied to the activity
     * configuration rather than inside Compose so that sheets, dialogs and pickers, which open
     * in windows of their own, follow it too. The display-size setting still scales the whole
     * interface evenly, which keeps proportions intact.
     */
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("pt-BR"))
            fontScale = 1f
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readReminder(intent)
        enableEdgeToEdge()
        setContent {
            AppThemeRoot(openTenantId = openTenant, openPeriod = openPeriod, onOpenConsumed = {
                openTenant = null; openPeriod = null
                intent.removeExtra(RentReminderScheduler.EXTRA_TENANT)
                intent.removeExtra(RentReminderScheduler.EXTRA_PERIOD)
            })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readReminder(intent)
    }
}

@Composable
private fun AppThemeRoot(
    settings: SettingsViewModel = hiltViewModel(),
    openTenantId: String? = null,
    openPeriod: String? = null,
    onOpenConsumed: () -> Unit = {}
) {
    val mode by settings.themeModeFlow.collectAsStateWithLifecycle()
    val dark = when (mode) { AppTheme.LIGHT -> false; AppTheme.DARK -> true; AppTheme.SYSTEM -> isSystemInDarkTheme() }
    RentalValidatorTheme(darkTheme = dark) { NaniApp(openTenantId, openPeriod, onOpenConsumed) }
}
