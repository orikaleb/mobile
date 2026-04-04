package com.example.nexiride2.domain.repository

import com.example.nexiride2.domain.model.PaymentMethod
import com.example.nexiride2.domain.model.User

interface UserRepository {
    suspend fun getUser(): Result<User>
    suspend fun updateUser(name: String, email: String, phone: String): Result<User>
    suspend fun updateProfilePhoto(photoUri: String): Result<User>
    suspend fun getSavedPaymentMethods(): Result<List<PaymentMethod>>
    suspend fun addPaymentMethod(paymentMethod: PaymentMethod): Result<Boolean>
    suspend fun removePaymentMethod(paymentMethodId: String): Result<Boolean>
}
