package com.example.nexiride2.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.PaymentMethod
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(val isLoading: Boolean = true, val user: User? = null, val paymentMethods: List<PaymentMethod> = emptyList(), val error: String? = null, val updateSuccess: Boolean = false)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()
    init { load() }

    fun load() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val user = repo.getUser().getOrNull()
        val payments = repo.getSavedPaymentMethods().getOrDefault(emptyList())
        _uiState.value = ProfileUiState(false, user, payments)
    }

    fun updateProfile(name: String, email: String, phone: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repo.updateUser(name, email, phone).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, user = it, updateSuccess = true) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun removePaymentMethod(id: String) = viewModelScope.launch { repo.removePaymentMethod(id); load() }
}
