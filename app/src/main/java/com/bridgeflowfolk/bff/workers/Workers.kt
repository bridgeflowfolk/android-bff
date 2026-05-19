package com.bridgeflowfolk.bff.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bridgeflowfolk.bff.data.local.EventDao
import com.bridgeflowfolk.bff.data.toDomain
import com.bridgeflowfolk.bff.domain.EventRepository
import com.bridgeflowfolk.bff.domain.UserPreferencesRepository
import com.bridgeflowfolk.bff.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ─── SyncWorker : synchronisation périodique (durée configurable) ─────────────

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repository: EventRepository,
    private val notificationHelper: NotificationHelper,
    private val eventDao: EventDao,
    private val prefsRepository: UserPreferencesRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val newIds = repository.syncFromNetwork()

            if (newIds.isNotEmpty()) {
                val newEvents = newIds.mapNotNull { eventDao.findById(it) }.map { it.toDomain() }
                notificationHelper.notifyNewEvents(newEvents)
            }

            scheduleReminders()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun scheduleReminders() {
        val prefs = prefsRepository.prefsFlow.first()
        val reminderHours = prefs.reminderHoursBefore.toLong().coerceAtLeast(1L)
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val upcoming = eventDao.upcomingWithoutReminder(now)

        upcoming.forEach { entity ->
            val eventTime = LocalDateTime.parse(entity.date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val reminderTime = eventTime.minusHours(reminderHours)
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
        const val WORK_NAME    = "bff_sync_periodic"

        /** Replanifie avec la durée stockée dans les préférences */
        fun schedule(context: Context, intervalHours: Long = 6L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalHours.coerceIn(1L, 24L), TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,   // UPDATE pour prendre la nouvelle durée
                request
            )
        }
    }
}

// ─── ReminderWorker ───────────────────────────────────────────────────────────

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val eventDao: EventDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val id     = inputData.getString(SyncWorker.KEY_EVENT_ID) ?: return Result.failure()
        val entity = eventDao.findById(id) ?: return Result.failure()
        notificationHelper.notifyReminder(entity.toDomain())
        return Result.success()
    }
}

// ─── BootReceiver ─────────────────────────────────────────────────────────────

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SyncWorker.schedule(context)
        }
    }
}
