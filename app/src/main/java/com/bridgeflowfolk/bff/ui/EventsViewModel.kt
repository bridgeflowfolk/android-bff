package com.bridgeflowfolk.bff.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bridgeflowfolk.bff.domain.Event
import com.bridgeflowfolk.bff.domain.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EventsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading   = MutableStateFlow(false)
    private val _error       = MutableStateFlow<String?>(null)

    /** Flux d'événements réactif : switch selon la recherche en cours */
    val uiState: StateFlow<EventsUiState> = combine(
        _searchQuery
            .debounce(300)
            .flatMapLatest { q ->
                if (q.isBlank()) repository.observeEvents()
                else repository.searchEvents(q)
            },
        _isLoading,
        _error,
        _searchQuery
    ) { events, loading, error, query ->
        EventsUiState(events = events, isLoading = loading, error = error, searchQuery = query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState(isLoading = true)
    )

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }

    /** Pull-to-refresh manuel */
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

    init { refresh() }   // sync au démarrage
}
