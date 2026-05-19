package com.bridgeflowfolk.bff.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bridgeflowfolk.bff.domain.Event
import com.bridgeflowfolk.bff.domain.EventRepository
import com.bridgeflowfolk.bff.domain.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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

    val uiState: StateFlow<EventsUiState> = combine(
        _searchQuery
            .debounce(300)
            .flatMapLatest { q ->
                if (q.isBlank()) repository.observeEvents()
                else repository.searchEvents(q)
            },
        _isLoading,
        _error,
        _searchQuery,
        _hidePassedEvents
    ) { events, loading, error, query, hideP ->
        val filtered = if (hideP) {
            val now = LocalDateTime.now()
            events.filter { it.dateTime.isAfter(now) }
        } else events
        EventsUiState(
            events = filtered,
            isLoading = loading,
            error = error,
            searchQuery = query,
            hidePassedEvents = hideP
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState(isLoading = true)
    )

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }

    fun onToggleHidePassedEvents(hide: Boolean) { _hidePassedEvents.value = hide }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.syncFromNetwork()
            } catch (e: Exception) {
                _error.value = "Impossible de synchroniser. Vérifiez votre connexion."
            } finally {
                _isLoading.value = false
            }
        }
    }

    init { refresh() }
}

// ─── ViewModel préférences notifications ─────────────────────────────────────

data class NotifPrefsUiState(
    val syncIntervalHours: Float = 6f,
    val reminderHoursBefore: Float = 2f
)

@HiltViewModel
class NotifPrefsViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<NotifPrefsUiState> = prefsRepository.prefsFlow
        .map { NotifPrefsUiState(it.syncIntervalHours, it.reminderHoursBefore) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotifPrefsUiState()
        )

    fun setSyncInterval(hours: Float) {
        viewModelScope.launch { prefsRepository.setSyncInterval(hours) }
    }

    fun setReminderHoursBefore(hours: Float) {
        viewModelScope.launch { prefsRepository.setReminderHoursBefore(hours) }
    }
}
