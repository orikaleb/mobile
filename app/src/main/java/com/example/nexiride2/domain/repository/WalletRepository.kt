package com.example.nexiride2.domain.repository

/**
 * Demo in-app balance only (Room on device). Not linked to real PSPs or Firestore money.
 */
interface WalletRepository {
    suspend fun getBalanceGhs(): Result<Double>
    suspend fun topUp(amountGhs: Double): Result<Double>
    suspend fun tryDebit(amountGhs: Double): Result<Unit>
}
