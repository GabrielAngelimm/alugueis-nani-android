package com.rentalvalidator.app.reminders

import android.annotation.SuppressLint
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/** Existing device-local format. Call on IO, under the scheduler's mutex. */
internal class RentReminderStore(private val preferences: SharedPreferences) {
    fun ids(): List<String> = preferences.all.keys.toList()

    fun read(id: String): RentReminder? {
        val raw = runCatching { preferences.getString(id, null) }.getOrNull() ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val version = json.optInt("version", 0)
        // A newer app may have written this entry. Preserve it without interpreting it.
        if (version !in 0..CURRENT_VERSION) return null
        val value = runCatching {
            RentReminder(json.getInt("hours"), json.getInt("day"), json.getLong("at"),
                json.getString("due"), json.getString("zone")).also {
                require(it.leadHours in 1..672 && it.dueDay in 1..31 && it.triggerAt > 0)
                LocalDate.parse(it.dueDate)
                ZoneId.of(it.zone)
            }
        }.getOrNull() ?: return null
        if (version == 0) write(id, value)
        return value
    }

    // Check commit's result before scheduling. KTX edit returns Unit and hides this result.
    @SuppressLint("UseKtx")
    fun write(id: String, value: RentReminder) {
        val json = JSONObject().put("version", CURRENT_VERSION).put("hours", value.leadHours).put("day", value.dueDay)
            .put("at", value.triggerAt).put("due", value.dueDate).put("zone", value.zone)
        check(preferences.edit().putString(id, json.toString()).commit()) { "Não foi possível salvar o lembrete." }
    }

    private companion object { const val CURRENT_VERSION = 1 }

    @SuppressLint("UseKtx")
    fun remove(id: String) {
        check(preferences.edit().remove(id).commit()) { "Não foi possível remover o lembrete." }
    }
}
