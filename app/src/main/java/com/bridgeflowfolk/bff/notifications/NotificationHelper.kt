package com.bridgeflowfolk.bff.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bridgeflowfolk.bff.BffApplication.Companion.CHANNEL_NEW_EVENTS
import com.bridgeflowfolk.bff.BffApplication.Companion.CHANNEL_REMINDERS
import com.bridgeflowfolk.bff.MainActivity
import com.bridgeflowfolk.bff.R
import com.bridgeflowfolk.bff.domain.Event
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val frFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.FRANCE)

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    private fun mainPendingIntent(requestCode: Int = 0) = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun notifyNewEvents(events: List<Event>) {
        if (!hasPermission() || events.isEmpty()) return
        val title = if (events.size == 1) "Nouvel événement BFF 🎉"
                    else "${events.size} nouveaux événements BFF 🎉"
        val body  = events.take(3).joinToString(", ") { it.title }

        manager.notify(
            NOTIF_ID_NEW_EVENTS,
            NotificationCompat.Builder(context, CHANNEL_NEW_EVENTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(mainPendingIntent())
                .build()
        )
    }

    /** Rappel X heures avant (durée issue des préférences) */
    fun notifyReminder(event: Event, hoursBefore: Long = 2L) {
        if (!hasPermission()) return
        val body = "📍 ${event.location} · ${event.dateTime.format(frFormatter)}"
        manager.notify(
            NOTIF_ID_REMINDER_BASE + event.id.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Dans ${hoursBefore}h : ${event.title}")
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(mainPendingIntent(event.id.hashCode()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    companion object {
        private const val NOTIF_ID_NEW_EVENTS   = 1001
        private const val NOTIF_ID_REMINDER_BASE = 2000
    }
}
