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
import dagger.hilt.android.qualifiers.ApplicationContext // AJOUT : Import requis
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context // AJOUT : Annotation @ApplicationContext
) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val frFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.FRANCE)

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    /** Notifie l'arrivée d'un ou plusieurs nouveaux événements */
    fun notifyNewEvents(events: List<Event>) {
        if (!hasPermission() || events.isEmpty()) return

        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (events.size == 1)
            "Nouvel événement BFF 🎉"
        else
            "${events.size} nouveaux événements BFF 🎉"

        val body = if (events.size == 1)
            events.first().title
        else
            events.take(3).joinToString(", ") { it.title }

        val notif = NotificationCompat.Builder(context, CHANNEL_NEW_EVENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()

        manager.notify(NOTIF_ID_NEW_EVENTS, notif)
    }

    /** Rappel 2h avant un événement spécifique */
    fun notifyReminder(event: Event) {
        if (!hasPermission()) return

        val intent = PendingIntent.getActivity(
            context, event.id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val body = "📍 ${event.location} · ${event.dateTime.format(frFormatter)}"

        val notif = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dans 2h : ${event.title}")
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIF_ID_REMINDER_BASE + event.id.hashCode(), notif)
    }

    companion object {
        private const val NOTIF_ID_NEW_EVENTS = 1001
        private const val NOTIF_ID_REMINDER_BASE = 2000
    }
}
