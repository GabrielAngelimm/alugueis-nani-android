package com.rentalvalidator.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

data class PenaltyConfig(
    val penaltyPct: Double,
    val dailyInterestPct: Double
)

data class BackupPreferences(
    val theme: AppTheme,
    val penaltyPct: Double,
    val dailyInterestPct: Double
)

@Singleton
class PreferencesManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val PENALTY_PCT_KEY = doublePreferencesKey("penalty_pct")
        val DAILY_INTEREST_PCT_KEY = doublePreferencesKey("daily_interest_pct")
    }

    val themeModeFlow: Flow<AppTheme> = dataStore.data.map { preferences ->
        val themeString = preferences[THEME_KEY] ?: AppTheme.LIGHT.name
        try {
            AppTheme.valueOf(themeString)
        } catch (e: Exception) {
            AppTheme.LIGHT
        }
    }

    val penaltyConfigFlow: Flow<PenaltyConfig> = dataStore.data.map { preferences ->
        val penaltyPct = preferences[PENALTY_PCT_KEY] ?: 20.0
        val dailyInterestPct = preferences[DAILY_INTEREST_PCT_KEY] ?: 0.0
        PenaltyConfig(penaltyPct, dailyInterestPct)
    }

    suspend fun setThemeMode(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun updatePenaltyConfig(penaltyPct: Double, dailyInterestPct: Double) {
        require(penaltyPct.isFinite() && penaltyPct >= 0.0)
        require(dailyInterestPct.isFinite() && dailyInterestPct >= 0.0)
        dataStore.edit { preferences ->
            preferences[PENALTY_PCT_KEY] = penaltyPct
            preferences[DAILY_INTEREST_PCT_KEY] = dailyInterestPct
        }
    }

    suspend fun resetPenaltyConfig() {
        dataStore.edit { preferences ->
            preferences.remove(PENALTY_PCT_KEY)
            preferences.remove(DAILY_INTEREST_PCT_KEY)
        }
    }

    suspend fun backupSnapshot(): BackupPreferences {
        val current = dataStore.data.first()
        return BackupPreferences(
            theme = runCatching { AppTheme.valueOf(current[THEME_KEY] ?: AppTheme.LIGHT.name) }
                .getOrDefault(AppTheme.LIGHT),
            penaltyPct = current[PENALTY_PCT_KEY] ?: 20.0,
            dailyInterestPct = current[DAILY_INTEREST_PCT_KEY] ?: 0.0
        )
    }

    suspend fun restoreBackup(value: BackupPreferences) {
        require(value.penaltyPct.isFinite() && value.penaltyPct >= 0.0)
        require(value.dailyInterestPct.isFinite() && value.dailyInterestPct >= 0.0)
        dataStore.edit { current ->
            current[THEME_KEY] = value.theme.name
            current[PENALTY_PCT_KEY] = value.penaltyPct
            current[DAILY_INTEREST_PCT_KEY] = value.dailyInterestPct
        }
    }
}
