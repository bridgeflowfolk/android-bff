package com.bridgeflowfolk.bff.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bridgeflowfolk.bff.data.local.EventDao
import com.bridgeflowfolk.bff.data.toDomain
import com.bridgeflowfolk.bff.domain.EventRepository
import com.bridgeflowfolk.bff.domain.InAppNotificationRepository
import com.bridgeflowfolk.bff.domain.UserPreferencesRepository
import com.bridgeflowfolk.bff.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ─── SyncWorker : synchronisation périodique des événements ──────────────────

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
            val prefs  = prefsRepository.prefsFlow.first()
            val newIds = repository.syncFromNetwork()

            if (prefs.notificationsEnabled && newIds.isNotEmpty()) {
                val newEvents = newIds.mapNotNull { eventDao.findById(it) }.map { it.toDomain() }
                notificationHelper.notifyNewEvents(newEvents)
            }

            if (prefs.notificationsEnabled) {
                scheduleReminders(prefs.reminderHoursBefore.toLong().coerceIn(1L, 24L))
            }

            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "SyncWorker échoué (tentative $runAttemptCount) : ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun scheduleReminders(reminderHours: Long) {
        val workManager = WorkManager.getInstance(applicationContext)
        val nowStr      = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // Passe 1 : détecter les jobs WorkManager annulés et remettre leur flag DB à false.
        // Cela permet la replanification après cancelAllWorkByTag (changement de délai).
        val allUpcoming = eventDao.upcomingAll(nowStr)
        allUpcoming.forEach { entity ->
            if (entity.reminderScheduled) {
                val existing    = workManager.getWorkInfosForUniqueWork("reminder_${entity.id}").get()
                val stillActive = existing.any {
                    it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
                }
                if (!stillActive) {
                    eventDao.setReminderScheduled(entity.id, false)
                }
            }
        }

        // Passe 2 : relit la liste (flags à jour) et planifie les manquants
        val toSchedule = eventDao.upcomingAll(nowStr)
        toSchedule.forEach { entity ->
            val uniqueName     = "reminder_${entity.id}"
            val existingWork   = workManager.getWorkInfosForUniqueWork(uniqueName).get()
            val alreadyPending = existingWork.any { info ->
                info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING
            }

            if (alreadyPending) {
                if (!entity.reminderScheduled) eventDao.markReminderScheduled(entity.id)
                return@forEach
            }

            val eventTime    = LocalDateTime.parse(entity.date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val reminderTime = eventTime.minusHours(reminderHours)
            val delayMs      = reminderTime
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()

            if (delayMs > 0) {
                val reminderWork = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInputData(workDataOf(
                        KEY_EVENT_ID     to entity.id,
                        KEY_HOURS_BEFORE to reminderHours
                    ))
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag(TAG_REMINDER)
                    .build()

                workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.APPEND_OR_REPLACE, reminderWork)
                eventDao.markReminderScheduled(entity.id)
                Log.d(TAG, "Rappel planifié pour ${entity.id} dans ${delayMs / 60_000} min")
            } else {
                eventDao.markReminderScheduled(entity.id)
            }
        }
    }

    companion object {
        private const val TAG      = "SyncWorker"
        const val KEY_EVENT_ID     = "event_id"
        const val KEY_HOURS_BEFORE = "hours_before"
        const val WORK_NAME        = "bff_sync_periodic"
        const val TAG_REMINDER     = "bff_reminder"

        fun schedule(context: Context, intervalHours: Long = 6L) {
            val safeInterval = intervalHours.coerceIn(1L, 24L)
            val constraints  = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(safeInterval, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
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
    private val notificationHelper: NotificationHelper,
    private val prefsRepository: UserPreferencesRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val prefs = prefsRepository.prefsFlow.first()
        if (!prefs.notificationsEnabled) return Result.success()

        val id     = inputData.getString(SyncWorker.KEY_EVENT_ID)  ?: return Result.failure()
        val hours  = inputData.getLong(SyncWorker.KEY_HOURS_BEFORE, 2L)
        val entity = eventDao.findById(id)                          ?: return Result.failure()
        notificationHelper.notifyReminder(entity.toDomain(), hours)
        return Result.success()
    }
}

// ─── NotifSyncWorker : fetch périodique des notifications in-app ──────────────

@HiltWorker
class NotifSyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val notifRepository: InAppNotificationRepository,
    private val notificationHelper: NotificationHelper,
    private val prefsRepository: UserPreferencesRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs  = prefsRepository.prefsFlow.first()
            val newIds = notifRepository.syncFromNetwork()  // silencieux si pas de réseau

            if (prefs.notificationsEnabled && newIds.isNotEmpty()) {
                val allNotifs = notifRepository.observeAll().first()
                val newNotifs = allNotifs.filter { it.id in newIds }
                notificationHelper.notifyNewInAppNotifications(newNotifs)
            }

            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "NotifSyncWorker inattendu : ${e.message}")
            Result.success()  // Pas de retry : erreur non réseau = inattendue, on skip
        }
    }

    companion object {
        private const val TAG   = "NotifSyncWorker"
        const val WORK_NAME     = "bff_notif_sync_periodic"

        fun schedule(context: Context, intervalHours: Long = 4L) {
            val safeInterval = intervalHours.coerceIn(1L, 24L)
            val constraints  = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<NotifSyncWorker>(safeInterval, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

// ─── BootReceiver ─────────────────────────────────────────────────────────────

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,              // Android standard
            "android.intent.action.QUICKBOOT_POWERON", // Xiaomi MIUI
            "com.htc.intent.action.QUICKBOOT_POWERON"  // HTC Sense
            -> {
                SyncWorker.schedule(context)
                NotifSyncWorker.schedule(context)
            }
        }
    }
}
