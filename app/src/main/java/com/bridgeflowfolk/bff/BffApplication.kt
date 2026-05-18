package com.bridgeflowfolk.bff

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BffApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /** WorkManager configuré avec Hilt (injection dans les Workers) */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Canal : nouveaux événements
            NotificationChannel(
                CHANNEL_NEW_EVENTS,
                "Nouveaux événements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notification quand un nouvel événement BFF est publié"
                manager.createNotificationChannel(this)
            }

            // Canal : rappels avant événement
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Rappels d'événements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rappel automatique 2h avant un événement"
                manager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_NEW_EVENTS = "bff_new_events"
        const val CHANNEL_REMINDERS = "bff_reminders"
    }
}
