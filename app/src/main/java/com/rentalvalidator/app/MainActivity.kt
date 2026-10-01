package com.rentalvalidator.app
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentalvalidator.app.data.local.datastore.AppTheme
import com.rentalvalidator.app.presentation.theme.RentalValidatorTheme
import com.rentalvalidator.app.presentation.viewmodel.SettingsViewModel
import com.rentalvalidator.app.presentation.navigation.NaniApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity:ComponentActivity() {
    private var openTenant by mutableStateOf<String?>(null)
    private var openPeriod by mutableStateOf<String?>(null)
    private fun readReminder(intent: android.content.Intent) {
        openTenant = intent.getStringExtra(com.rentalvalidator.app.reminders.RentReminderScheduler.EXTRA_TENANT)
        openPeriod = intent.getStringExtra(com.rentalvalidator.app.reminders.RentReminderScheduler.EXTRA_PERIOD)
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState==null) readReminder(intent)
        enableEdgeToEdge()
        setContent { AppThemeRoot(openTenantId=openTenant,openPeriod=openPeriod,onOpenConsumed={
            openTenant=null;openPeriod=null
            intent.removeExtra(com.rentalvalidator.app.reminders.RentReminderScheduler.EXTRA_TENANT)
            intent.removeExtra(com.rentalvalidator.app.reminders.RentReminderScheduler.EXTRA_PERIOD)
        }) }
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); setIntent(intent); readReminder(intent) }
}
@Composable
private fun AppThemeRoot(settings:SettingsViewModel=hiltViewModel(),openTenantId:String?=null,openPeriod:String?=null,onOpenConsumed:()->Unit={}) {
    val mode by settings.themeModeFlow.collectAsStateWithLifecycle()
    val dark=when(mode) { AppTheme.LIGHT->false;AppTheme.DARK->true;AppTheme.SYSTEM->isSystemInDarkTheme() }
    RentalValidatorTheme(darkTheme=dark) { NaniApp(openTenantId,openPeriod,onOpenConsumed) }
}
@Composable fun MainApp() = NaniApp()
