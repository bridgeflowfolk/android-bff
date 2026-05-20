package com.bridgeflowfolk.bff.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bridgeflowfolk.bff.domain.Event
import com.bridgeflowfolk.bff.domain.EventRepository
import com.bridgeflowfolk.bff.domain.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery      = MutableStateFlow("")
    private val _isLoading        = MutableStateFlow(false)
    private val _error            = MutableStateFlow<String?>(null)
    private val _hidePassedEvents = MutableStateFlow(false)

    // Ticker toutes les 60s pour rafraîchir le filtre "passés" sans appel réseau.
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
        // WhileSubscribed(5_000) : coupe la collecte 5s après que l'UI passe en arrière-plan,
        // en cohérence avec collectAsStateWithLifecycle côté Composable.
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState(isLoading = true)
    )

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }

    fun onToggleHidePassedEvents(hide: Boolean) { _hidePassedEvents.value = hide }

    /**
     * Suspend jusqu'à la fin réelle du sync réseau.
     * PullToRefreshBox attend cette fonction avant de masquer le spinner.
     */
    suspend fun refreshSuspending() {
        _isLoading.value = true
        _error.value     = null
        try {
            repository.syncFromNetwork()
        } catch (e: Exception) {
            _error.value = "Impossible de synchroniser. Vérifiez votre connexion."
        } finally {
            _isLoading.value = false
        }
    }

    /** Version fire-and-forget pour l'init du ViewModel. */
    fun refresh() {
        viewModelScope.launch { refreshSuspending() }
    }

    init { refresh() }
}

// ─── ViewModel préférences notifications ─────────────────────────────────────

data class NotifPrefsUiState(
    val syncIntervalHours: Float      = 6f,
    val reminderHoursBefore: Float    = 2f,
    val notificationsEnabled: Boolean = true
)

@HiltViewModel
class NotifPrefsViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<NotifPrefsUiState> = prefsRepository.prefsFlow
        .map { NotifPrefsUiState(it.syncIntervalHours, it.reminderHoursBefore, it.notificationsEnabled) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotifPrefsUiState()
        )

    fun setSyncInterval(hours: Float) {
        viewModelScope.launch { 
            prefsRepository.setSyncInterval(hours)            
            com.bridgeflowfolk.bff.workers.SyncWorker.schedule(context)
        }
    }

    fun setReminderHoursBefore(hours: Float) {
        viewModelScope.launch { 
            prefsRepository.setReminderHoursBefore(hours)           
            com.bridgeflowfolk.bff.workers.SyncWorker.rescheduleAllReminders(
                context = context,
                eventDao = eventDao,
                hoursBefore = hours.toLong()
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setNotificationsEnabled(enabled) }
    }
}
