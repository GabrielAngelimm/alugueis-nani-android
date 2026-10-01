package com.rentalvalidator.app.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RentReminderStoreTest {
    @Test fun legacyEntriesMigrateWithoutChangingTheSchedule() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "store-test-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            val legacy = JSONObject().put("hours", 48).put("day", 12).put("at", 1789200000000L)
                .put("due", "2026-09-12").put("zone", "America/Sao_Paulo")
            assertTrue(prefs.edit().putString("tenant", legacy.toString()).commit())
            val store = RentReminderStore(prefs)
            val value = store.read("tenant")!!
            assertEquals(48, value.leadHours)
            assertEquals(1789200000000L, value.triggerAt)
            assertEquals("2026-09-12", value.dueDate)
            assertEquals(1, JSONObject(prefs.getString("tenant", null)!!).getInt("version"))
            assertEquals(value, RentReminderStore(prefs).read("tenant"))

            val future = legacy.put("version", 99).toString()
            assertTrue(prefs.edit().putString("future", future).commit())
            assertNull(store.read("future"))
            assertEquals(future, prefs.getString("future", null))
            assertTrue(prefs.edit().putString("corrupt", "not json").commit())
            assertNull(store.read("corrupt"))
            assertTrue(prefs.edit().putInt("wrong-type", 42).commit())
            assertNull(store.read("wrong-type"))
            listOf("hours" to 0, "day" to 32, "due" to "invalid", "zone" to "invalid").forEach { (key, invalid) ->
                val bad = JSONObject(legacy.toString()).put("version", 1).put(key, invalid).toString()
                assertTrue(prefs.edit().putString("bad-$key", bad).commit())
                assertNull(store.read("bad-$key"))
                assertEquals(bad, prefs.getString("bad-$key", null))
            }
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
