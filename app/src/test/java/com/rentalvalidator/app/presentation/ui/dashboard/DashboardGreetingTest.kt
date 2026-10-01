package com.rentalvalidator.app.presentation.ui.dashboard

import com.rentalvalidator.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardGreetingTest {
    @Test
    fun greetingTracksLocalTimeBoundaries() {
        assertEquals(R.string.greeting_evening, dashboardGreetingResource(4))
        assertEquals(R.string.greeting_morning, dashboardGreetingResource(5))
        assertEquals(R.string.greeting_morning, dashboardGreetingResource(11))
        assertEquals(R.string.greeting_afternoon, dashboardGreetingResource(12))
        assertEquals(R.string.greeting_afternoon, dashboardGreetingResource(17))
        assertEquals(R.string.greeting_evening, dashboardGreetingResource(18))
    }
}
