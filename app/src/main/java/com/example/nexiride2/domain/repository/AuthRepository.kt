package com.example.nexiride2.domain.repository

import com.example.nexiride2.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signUp(name: String, email: String, phone: String, password: String): Result<User>
    suspend fun forgotPassword(email: String): Result<Boolean>
    suspend fun verifyOtp(email: String, otp: String): Result<Boolean>
    suspend fun resetPassword(email: String, newPassword: String): Result<Boolean>
    suspend fun logout()
    fun isLoggedIn(): Boolean
    fun getCurrentUser(): User?

    /**
     * Live stream of the currently signed-in user (null when signed out).
     * Emits the current value immediately on subscription and again every time
     * Firebase/Mock auth state changes (sign-in, sign-out, account switch, token
     * revocation). Downstream ViewModels rely on this to refresh/clear their
     * cached per-user data when the account identity changes.
     */
    fun observeCurrentUser(): Flow<User?>
}
