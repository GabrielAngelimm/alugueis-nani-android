package com.rentalvalidator.app.reminders

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.rentalvalidator.app.MainActivity
import org.junit.Rule
import org.junit.Test

/** Exercises the production NavHost without changing the user's database. */
class ReminderNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun notificationIntentOpensScopedPaymentsAtTheDuePeriod() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        fun intent(id: String, period: String) = Intent(context, MainActivity::class.java)
            .putExtra(RentReminderScheduler.EXTRA_TENANT, id)
            .putExtra(RentReminderScheduler.EXTRA_PERIOD, period)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        ActivityScenario.launch<MainActivity>(intent("missing-reminder-fixture", "2028-02")).use {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Fevereiro 2028").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Pagamentos").assertIsDisplayed()
            compose.onNodeWithText("Nenhum pagamento encontrado").assertIsDisplayed()
            context.startActivity(intent("another-missing-fixture", "2028-03"))
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Março 2028").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Nenhum pagamento encontrado").assertIsDisplayed()
        }
    }
}
