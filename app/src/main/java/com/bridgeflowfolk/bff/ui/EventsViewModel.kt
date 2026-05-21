package com.bridgeflowfolk.bff.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bridgeflowfolk.bff.domain.Event
import com.bridgeflowfolk.bff.domain.EventRepository
import com.bridgeflowfolk.bff.domain.InAppNotificationRepository
import com.bridgeflowfolk.bff.domain.UserPreferencesRepository
import com.bridgeflowfolk.bff.workers.NotifSyncWorker
import com.bridgeflowfolk.bff.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val hidePassedEvents: Boolean = false
)

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EventsViewModel @Inject constructor(
    private val repository: EventRepository,
    private val notifRepository: InAppNotificationRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery      = MutableStateFlow("")
    private val _isLoading        = MutableStateFlow(false)
    private val _error            = MutableStateFlow<String?>(null)
    private val _hidePassedEvents = MutableStateFlow(false)

    // Ticker toutes les 60s pour rafraîchir le filtre "passés" sans appel réseau
    private val _minuteTick: Flow<Long> = flow {
        var tick = 0L
        while (true) { emit(tick++); delay(60_000L) }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val _rawEvents: Flow<List<Event>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.observeEvents()
            else repository.searchEvents(q)
        }

    private val _filteredEvents: Flow<List<Event>> = combine(
        _rawEvents,
        _hidePassedEvents,
        _minuteTick
    ) { events, hideP, _ ->
        if (hideP) {
            val now = LocalDateTime.now()
            events.filter { it.dateTime.isAfter(now) }
        } else events
    }

    val uiState: StateFlow<EventsUiState> = combine(
        _filteredEvents,
        _isLoading,
        _error,
        _searchQuery,
        _hidePassedEvents
    ) { filtered, loading, error, query, hideP ->
        EventsUiState(
            events           = filtered,
            isLoading        = loading,
            error            = error,
            searchQuery      = query,
            hidePassedEvents = hideP
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState(isLoading = true)
    )

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }

    fun onToggleHidePassedEvents(hide: Boolean) { _hidePassedEvents.value = hide }

    suspend fun refreshSuspending() {
        _isLoading.value = true
        _error.value     = null
        try {
            // Fetch événements et notifications en parallèle — un seul spinner pour les deux
            val eventsDeferred = viewModelScope.async { repository.syncFromNetwork() }
            val notifsDeferred = viewModelScope.async { notifRepository.syncFromNetwork() }
            eventsDeferred.await()
            notifsDeferred.await()  // silencieux si pas de réseau (géré dans le repo)
        } catch (e: Exception) {
            _error.value = "Impossible de synchroniser. Vérifiez votre connexion."
        } finally {
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshSuspending() }
    }

    init { refresh() }
}

// ─── ViewModel préférences notifications ─────────────────────────────────────

data class NotifPrefsUiState(
    val syncIntervalHours: Float       = 6f,
    val reminderHoursBefore: Float     = 2f,
    val notificationsEnabled: Boolean  = true,
    val notifFetchIntervalHours: Float = 4f
)

@HiltViewModel
class NotifPrefsViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<NotifPrefsUiState> = prefsRepository.prefsFlow
        .map {
            NotifPrefsUiState(
                syncIntervalHours       = it.syncIntervalHours,
                reminderHoursBefore     = it.reminderHoursBefore,
                notificationsEnabled    = it.notificationsEnabled,
                notifFetchIntervalHours = it.notifFetchIntervalHours
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotifPrefsUiState()
        )

    /**
     * Met à jour l'intervalle de sync des événements ET replanifie le worker à chaud.
     * Le worker existant est remplacé grâce à ExistingPeriodicWorkPolicy.UPDATE.
     */
    fun setSyncInterval(hours: Float) {
        viewModelScope.launch {
            prefsRepository.setSyncInterval(hours)
            // Reschedule immédiat avec la nouvelle valeur — les rappels existants
            // dans WorkManager ne sont pas annulés (ils restent valables pour les
            // événements déjà connus avec l'ancienne date de rappel).
            SyncWorker.schedule(context, hours.toLong().coerceIn(1L, 24L))
        }
    }

    /**
     * Met à jour le délai de rappel ET replanifie tous les rappels futurs.
     * 1. Annule les jobs WorkManager par tag.
     * 2. Remet reminderScheduled = false en base pour que le prochain SyncWorker
     *    replanifie avec le nouveau délai.
     * 3. Déclenche un one-shot SyncWorker immédiat si le réseau est disponible.
     */
    fun setReminderHoursBefore(hours: Float) {
        viewModelScope.launch {
            prefsRepository.setReminderHoursBefore(hours)
            val wm  = androidx.work.WorkManager.getInstance(context)
            val now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            // Annuler les rappels WorkManager en attente
            wm.cancelAllWorkByTag(SyncWorker.TAG_REMINDER)
            // Remettre les flags en base (injection du DAO via un worker n'est pas nécessaire
            // car on peut accéder au DAO depuis un repository — ici on passe via le SyncWorker)
            // → Le one-shot SyncWorker ci-dessous appellera scheduleReminders() qui voit
            //   alreadyPending=false pour tous les rappels annulés et les replanifie.
            val oneShot = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .build()
            wm.enqueue(oneShot)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setNotificationsEnabled(enabled) }
    }

    /**
     * Met à jour l'intervalle de fetch des notifications in-app ET replanifie le worker.
     */
    fun setNotifFetchInterval(hours: Float) {
        viewModelScope.launch {
            prefsRepository.setNotifFetchInterval(hours)
            NotifSyncWorker.schedule(context, hours.toLong().coerceIn(1L, 24L))
        }
    }
}
