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
        // Validation explicite : évite un comportement silencieux si la valeur
        // dépasse la plage admissible (WorkManager min = 15 min, max raisonnable = 24h)
        require(reminderHours in 1..24) {
            "reminderHours doit être compris entre 1 et 24, reçu : $reminderHours"
        }

        val workManager = WorkManager.getInstance(applicationContext)
        val now = LocalDateTime.now()
        val nowStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // Tous les événements à venir — on vérifie l'état réel WorkManager
        // plutôt que de se fier uniquement au flag DB (peut être désynchronisé
        // après reboot / clear de données).
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
            val eventTime   = LocalDateTime.parse(entity.date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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

                // APPEND_OR_REPLACE : remplace un job FAILED/CANCELLED éventuel
                // sans tuer un job ENQUEUED/RUNNING (exclu ci-dessus)
                workManager.enqueueUniqueWork(
                    uniqueName,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    reminderWork
                )
                eventDao.markReminderScheduled(entity.id)
                Log.d(TAG, "Rappel planifié pour ${entity.id} dans ${delayMs / 60_000} min")
            } else {
                // Fenêtre passée mais événement à venir : marquer pour ne pas boucler
                eventDao.markReminderScheduled(entity.id)
            }
        }
    }

    companion object {
        private const val TAG           = "SyncWorker"
        const val KEY_EVENT_ID          = "event_id"
        const val KEY_HOURS_BEFORE      = "hours_before"
        const val WORK_NAME             = "bff_sync_periodic"
        const val TAG_REMINDER          = "bff_reminder"

        fun schedule(context: Context, intervalHours: Long = 6L) {
            require(intervalHours in 1..24) {
                "intervalHours doit être compris entre 1 et 24, reçu : $intervalHours"
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

       suspend fun rescheduleAllReminders(
        context: Context, 
        eventDao: com.bridgeflowfolk.bff.data.local.EventDao, 
        hoursBefore: Long
    ) {
        val workManager = WorkManager.getInstance(context)
        val nowStr = java.time.LocalDateTime.now().toString()

        // 1. Récupérer tous les événements futurs dans la base Room
        val upcomingEvents = eventDao.upcomingAll(nowStr)

        for (entity in upcomingEvents) {
            // 2. Annuler l'ancienne alarme de cet événement
            workManager.cancelUniqueWork("reminder_${entity.id}")

            // 3. Calculer le nouveau moment où déclencher l'alarme
            val eventDateTime = java.time.LocalDateTime.parse(entity.date)
            val reminderDateTime = eventDateTime.minusHours(hoursBefore)
            val delayMs = java.time.Duration.between(java.time.LocalDateTime.now(), reminderDateTime).toMillis()

            // 4. Si l'événement est encore dans le futur, on reprogramme le Worker
            if (delayMs > 0) {
                val inputData = Data.Builder()
                    .putString(KEY_EVENT_ID, entity.id)
                    .putLong(KEY_HOURS_BEFORE, hoursBefore)
                    .build()

                val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInputData(inputData)
                    .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()

                workManager.enqueueUniqueWork(
                    "reminder_${entity.id}",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
                
                // Mettre à jour le statut dans la base de données
                eventDao.setReminderScheduled(entity.id, true)
            } else {
                // Si avec le nouveau délai, l'alarme tombe dans le passé, on l'annule en base
                eventDao.setReminderScheduled(entity.id, false)
            }
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
// Déclaré avec android:permission="android.permission.RECEIVE_BOOT_COMPLETED"
// dans le Manifest → seul le système peut l'invoquer.

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" || 
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            SyncWorker.schedule(context)
        }
    }
}
