package com.bridgeflowfolk.bff.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bridgeflowfolk.bff.domain.InAppNotification
import com.bridgeflowfolk.bff.domain.InAppNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InAppNotifUiState(
    val notifications: List<InAppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class InAppNotificationViewModel @Inject constructor(
    private val repository: InAppNotificationRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<InAppNotifUiState> = combine(
        repository.observeAll(),
        repository.observeUnreadCount(),
        _isLoading
    ) { notifications, unread, loading ->
        InAppNotifUiState(
            notifications = notifications,
            unreadCount   = unread,
            isLoading     = loading
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = InAppNotifUiState(isLoading = true)
    )

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try { repository.syncFromNetwork() }
            finally { _isLoading.value = false }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { repository.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { repository.markAllRead() }
    }

    init { refresh() }
}
