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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            NotificationChannel(
                CHANNEL_NEW_EVENTS,
                "Nouveaux événements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notification quand un nouvel événement BFF est publié"
                manager.createNotificationChannel(this)
            }

            NotificationChannel(
                CHANNEL_REMINDERS,
                "Rappels d'événements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rappel automatique avant un événement"
                manager.createNotificationChannel(this)
            }

            // Canal dédié aux informations publiées via la cloche
            NotificationChannel(
                CHANNEL_IN_APP_NOTIFS,
                "Informations BFF",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Informations générales publiées par l'association"
                manager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_NEW_EVENTS    = "bff_new_events"
        const val CHANNEL_REMINDERS     = "bff_reminders"
        const val CHANNEL_IN_APP_NOTIFS = "bff_in_app_notifs"
    }
}
