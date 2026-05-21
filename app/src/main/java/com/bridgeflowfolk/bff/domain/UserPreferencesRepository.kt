package com.bridgeflowfolk.bff.domain

import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val syncIntervalHours: Float = 6f,
    val reminderHoursBefore: Float = 2f,
    val notificationsEnabled: Boolean = true,
    // Intervalle de fetch des notifications in-app (cloche), indépendant du sync événements
    val notifFetchIntervalHours: Float = 4f
)

interface UserPreferencesRepository {
    val prefsFlow: Flow<UserPreferences>
    suspend fun setSyncInterval(hours: Float)
    suspend fun setReminderHoursBefore(hours: Float)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotifFetchInterval(hours: Float)
}
