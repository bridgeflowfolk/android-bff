package com.bridgeflowfolk.bff.domain

import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val syncIntervalHours: Float = 6f,
    val reminderHoursBefore: Float = 2f,
    val notificationsEnabled: Boolean = true   // activation globale des notifications
)

interface UserPreferencesRepository {
    val prefsFlow: Flow<UserPreferences>
    suspend fun setSyncInterval(hours: Float)
    suspend fun setReminderHoursBefore(hours: Float)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
