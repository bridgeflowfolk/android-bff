package com.bridgeflowfolk.bff.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bridgeflowfolk.bff.data.local.EventDao
import com.bridgeflowfolk.bff.data.toDomain // Ajout de l'import direct
import com.bridgeflowfolk.bff.domain.EventRepository
import com.bridgeflowfolk.bff.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ─── SyncWorker : synchronisation toutes les 6h ──────────────────────────────

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repository: EventRepository,
    private val notificationHelper: NotificationHelper,
    private val eventDao: EventDao
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Sync réseau → Room, récupère les IDs nouveaux
            val newIds = repository.syncFromNetwork()

            // 2. Notifie si nouveaux événements
            if (newIds.isNotEmpty()) {
                val newEvents = newIds.mapNotNull { eventDao.findById(it) }
                    .map { toDomain(it) } // Utilisation propre de l'import
                notificationHelper.notifyNewEvents(newEvents)
            }

            // 3. Planifie les rappels pour les événements futurs
            scheduleReminders()

            Result.success()
        } catch (e: Exception) {
            // Retry avec backoff exponentiel (max 3 tentatives)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun scheduleReminders() {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val upcoming = eventDao.upcomingWithoutReminder(now)

        upcoming.forEach { entity ->
            val eventTime = LocalDateTime.parse(entity.date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val reminderTime = eventTime.minusHours(2)
            val delayMs = reminderTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() - System.currentTimeMillis()

            if (delayMs > 0) {
                val reminderWork = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInputData(workDataOf(KEY_EVENT_ID to entity.id))
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag("reminder_${entity.id}")
                    .build()

                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork(
                        "reminder_${entity.id}",
                        ExistingWorkPolicy.KEEP,
                        reminderWork
                    )

                eventDao.markReminderScheduled(entity.id)
            }
        }
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
        const val WORK_NAME = "bff_sync_periodic"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

// ─── ReminderWorker : envoie la notif de rappel ──────────────────────────────

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val eventDao: EventDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(SyncWorker.KEY_EVENT_ID) ?: return Result.failure()
        val entity = eventDao.findById(id) ?: return Result.failure()
        notificationHelper.notifyReminder(toDomain(entity)) // Utilisation propre de l'import
        return Result.success()
    }
}

// ─── BootReceiver : relance le WorkManager après reboot ──────────────────────

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SyncWorker.schedule(context)
        }
    }
}
