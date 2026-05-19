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

// ─── SyncWorker : synchronisation périodique ─────────────────────────────────

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
            val prefs = prefsRepository.prefsFlow.first()

            val newIds = repository.syncFromNetwork()

            if (prefs.notificationsEnabled && newIds.isNotEmpty()) {
                val newEvents = newIds.mapNotNull { eventDao.findById(it) }.map { it.toDomain() }
                notificationHelper.notifyNewEvents(newEvents)
            }

            if (prefs.notificationsEnabled) {
                scheduleReminders(prefs.reminderHoursBefore.toLong().coerceAtLeast(1L))
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun scheduleReminders(reminderHours: Long) {
        val workManager = WorkManager.getInstance(applicationContext)
        val now = LocalDateTime.now()
        val nowStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // Tous les événements à venir, qu'ils soient déjà marqués ou non en base.
        // On vérifie l'existence réelle du job dans WorkManager plutôt que de se fier
        // uniquement au flag DB (qui peut être désynchronisé après reboot / clear).
        val allUpcoming = eventDao.upcomingAll(nowStr)

        allUpcoming.forEach { entity ->
            val uniqueName = "reminder_${entity.id}"

            // Vérifier si un job est déjà enqueué et en attente dans WorkManager
            val existingWork = workManager.getWorkInfosForUniqueWork(uniqueName).get()
            val alreadyPending = existingWork.any { info ->
                info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING
            }

            if (alreadyPending) {
                // Job vivant dans WorkManager → s'assurer que le flag DB est cohérent
                if (!entity.reminderScheduled) eventDao.markReminderScheduled(entity.id)
                return@forEach
            }

            // Ici : job absent ou terminé → (re)planifier
            val eventTime = LocalDateTime.parse(entity.date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val reminderTime = eventTime.minusHours(reminderHours)
            val delayMs = reminderTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() - System.currentTimeMillis()

            if (delayMs > 0) {
                val reminderWork = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInputData(
                        workDataOf(
                            KEY_EVENT_ID     to entity.id,
                            KEY_HOURS_BEFORE to reminderHours
                        )
                    )
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag(TAG_REMINDER)
                    .build()

                // APPEND_OR_REPLACE : si un job du même nom existe (ex. état FAILED/CANCELLED),
                // il est remplacé ; s'il est ENQUEUED/RUNNING (cas normalement exclu ci-dessus),
                // il est conservé. Evite les doublons sans tuer un job valide.
                workManager.enqueueUniqueWork(
                    uniqueName,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    reminderWork
                )
                eventDao.markReminderScheduled(entity.id)
            } else {
                // La fenêtre de rappel est passée mais l'événement est encore à venir :
                // on marque quand même scheduled pour ne pas boucler dessus.
                eventDao.markReminderScheduled(entity.id)
            }
        }
    }

    companion object {
        const val KEY_EVENT_ID     = "event_id"
        const val KEY_HOURS_BEFORE = "hours_before"
        const val WORK_NAME        = "bff_sync_periodic"
        const val TAG_REMINDER     = "bff_reminder"

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

        val id     = inputData.getString(SyncWorker.KEY_EVENT_ID)      ?: return Result.failure()
        val hours  = inputData.getLong(SyncWorker.KEY_HOURS_BEFORE, 2L)
        val entity = eventDao.findById(id)                              ?: return Result.failure()
        notificationHelper.notifyReminder(entity.toDomain(), hours)
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
