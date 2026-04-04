package com.example.nexiride2.domain.repository

import com.example.nexiride2.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signUp(name: String, email: String, phone: String, password: String): Result<User>
    suspend fun forgotPassword(email: String): Result<Boolean>
    suspend fun verifyOtp(email: String, otp: String): Result<Boolean>
    suspend fun resetPassword(email: String, newPassword: String): Result<Boolean>
    suspend fun logout()
    fun isLoggedIn(): Boolean
    fun getCurrentUser(): User?
}
