package com.example.nexiride2.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.UserRepository
import com.example.nexiride2.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val walletBalanceGhs: Double? = null,
    val walletMessage: String? = null,
    val error: String? = null,
    val updateSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, walletMessage = null)
        val user = repo.getUser().getOrNull()
        val wallet = walletRepository.getBalanceGhs().getOrNull()
        _uiState.value = ProfileUiState(
            isLoading = false,
            user = user,
            walletBalanceGhs = wallet,
            walletMessage = null,
            error = null,
            updateSuccess = false
        )
    }

    fun topUpWallet(amountText: String) = viewModelScope.launch {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(walletMessage = "Enter a valid amount")
            return@launch
        }
        walletRepository.topUp(amount).fold(
            onSuccess = { newBal ->
                _uiState.value = _uiState.value.copy(
                    walletBalanceGhs = newBal,
                    walletMessage = "Added GHS ${"%.2f".format(amount)}"
                )
            },
            onFailure = { _uiState.value = _uiState.value.copy(walletMessage = it.message ?: "Could not add funds") }
        )
    }

    fun clearWalletMessage() {
        _uiState.value = _uiState.value.copy(walletMessage = null)
    }

    fun clearUpdateSuccess() {
        _uiState.value = _uiState.value.copy(updateSuccess = false)
    }

    fun updateProfile(name: String, email: String, phone: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repo.updateUser(name, email, phone).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, user = it, updateSuccess = true) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }
}
