package com.bridgeflowfolk.bff.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bridgeflowfolk.bff.domain.UserPreferences
import com.bridgeflowfolk.bff.domain.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bff_prefs")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object Keys {
        val SYNC_INTERVAL        = floatPreferencesKey("sync_interval_hours")
        val REMINDER_BEFORE      = floatPreferencesKey("reminder_hours_before")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    override val prefsFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            syncIntervalHours      = prefs[Keys.SYNC_INTERVAL]          ?: 6f,
            reminderHoursBefore    = prefs[Keys.REMINDER_BEFORE]        ?: 2f,
            notificationsEnabled   = prefs[Keys.NOTIFICATIONS_ENABLED]  ?: true
        )
    }

    override suspend fun setSyncInterval(hours: Float) {
        context.dataStore.edit { it[Keys.SYNC_INTERVAL] = hours.coerceIn(1f, 24f) }
    }

    override suspend fun setReminderHoursBefore(hours: Float) {
        context.dataStore.edit { it[Keys.REMINDER_BEFORE] = hours.coerceIn(1f, 24f) }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }
}
