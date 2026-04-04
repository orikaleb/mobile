package com.example.nexiride2.data.repository

import com.example.nexiride2.data.local.MockData
import com.example.nexiride2.domain.model.PaymentMethod
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.UserRepository
import kotlinx.coroutines.delay

class MockUserRepository : UserRepository {
    private var user = MockData.currentUser

    override suspend fun getUser(): Result<User> { delay(200); return Result.success(user) }
    override suspend fun updateUser(name: String, email: String, phone: String): Result<User> {
        delay(500); user = user.copy(name = name, email = email, phone = phone); return Result.success(user)
    }
    override suspend fun updateProfilePhoto(photoUri: String): Result<User> {
        delay(500); user = user.copy(profilePhotoUrl = photoUri); return Result.success(user)
    }
    override suspend fun getSavedPaymentMethods(): Result<List<PaymentMethod>> {
        delay(300); return Result.success(user.paymentMethods)
    }
    override suspend fun addPaymentMethod(paymentMethod: PaymentMethod): Result<Boolean> {
        delay(500); user = user.copy(paymentMethods = user.paymentMethods + paymentMethod); return Result.success(true)
    }
    override suspend fun removePaymentMethod(paymentMethodId: String): Result<Boolean> {
        delay(300); user = user.copy(paymentMethods = user.paymentMethods.filter { it.id != paymentMethodId }); return Result.success(true)
    }
}
