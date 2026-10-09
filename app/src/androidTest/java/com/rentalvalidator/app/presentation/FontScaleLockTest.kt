package com.rentalvalidator.app.presentation

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.rentalvalidator.app.MainActivity
import com.rentalvalidator.app.reminders.RentReminderScheduler
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/**
 * The app draws text at its designed size whatever the system font-size setting is. Run it with
 * `settings put system font_scale` above 1 to exercise the lock; at 1 it still checks the wiring.
 * It opens the payments page of a tenant that does not exist, so no user records are read or changed.
 */
class FontScaleLockTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun textKeepsTheDesignedSizeRegardlessOfTheSystemSetting() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(RentReminderScheduler.EXTRA_TENANT, "font-scale-fixture")
            .putExtra(RentReminderScheduler.EXTRA_PERIOD, "2028-02")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Fevereiro 2028").fetchSemanticsNodes().isNotEmpty() }
            scenario.onActivity { activity ->
                Assert.assertEquals(1f, activity.resources.configuration.fontScale, 0f)
                Assert.assertEquals("pt", activity.resources.configuration.locales[0].language)
            }
        }
    }
}
