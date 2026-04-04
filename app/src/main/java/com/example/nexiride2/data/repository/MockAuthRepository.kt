package com.example.nexiride2.data.repository

import com.example.nexiride2.data.local.MockData
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepository(
    private val tokenStore: SecureTokenStore
) : AuthRepository {
    private var loggedInUser: User? = if (tokenStore.getToken() != null) MockData.currentUser else null

    override suspend fun login(email: String, password: String): Result<User> {
        delay(800)
        return if (email.isNotBlank() && password.length >= 4) {
            tokenStore.putToken("mock_jwt_${System.currentTimeMillis()}")
            Result.success(MockData.currentUser.also { loggedInUser = it })
        }
        else Result.failure(Exception("Invalid credentials"))
    }

    override suspend fun signUp(name: String, email: String, phone: String, password: String): Result<User> {
        delay(1000)
        val user = MockData.currentUser.copy(name = name, email = email, phone = phone)
        tokenStore.putToken("mock_jwt_${System.currentTimeMillis()}")
        loggedInUser = user
        return Result.success(user)
    }

    override suspend fun forgotPassword(email: String): Result<Boolean> { delay(500); return Result.success(true) }
    override suspend fun verifyOtp(email: String, otp: String): Result<Boolean> { delay(500); return Result.success(otp == "1234") }
    override suspend fun resetPassword(email: String, newPassword: String): Result<Boolean> { delay(500); return Result.success(true) }
    override suspend fun logout() {
        tokenStore.clear()
        loggedInUser = null
    }

    override fun isLoggedIn() = tokenStore.getToken() != null
    override fun getCurrentUser() = loggedInUser
}
