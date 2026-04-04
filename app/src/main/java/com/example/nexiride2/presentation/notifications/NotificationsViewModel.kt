package com.example.nexiride2.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.AppNotification
import com.example.nexiride2.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()
    init { load() }

    fun load() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        repo.getNotifications().fold(
            onSuccess = { _uiState.value = NotificationsUiState(false, it, repo.getUnreadCount()) },
            onFailure = { _uiState.value = NotificationsUiState(false, error = it.message) }
        )
    }

    fun markAsRead(id: String) = viewModelScope.launch { repo.markAsRead(id); load() }
    fun markAllRead() = viewModelScope.launch { repo.markAllAsRead(); load() }
}
