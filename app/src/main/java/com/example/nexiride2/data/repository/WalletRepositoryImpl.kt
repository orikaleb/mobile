package com.example.nexiride2.data.repository

import com.example.nexiride2.data.local.db.WalletDao
import com.example.nexiride2.data.local.db.WalletEntity
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round as roundDouble

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val walletDao: WalletDao,
    private val authRepository: AuthRepository
) : WalletRepository {

    companion object {
        private const val DEMO_START_GHS = 200.0
    }

    private fun currentUserId(): String? = authRepository.getCurrentUser()?.id?.takeIf { it.isNotBlank() }

    private fun roundMoney(v: Double): Double = roundDouble(v * 100.0) / 100.0

    override suspend fun getBalanceGhs(): Result<Double> {
        val uid = currentUserId() ?: return Result.failure(Exception("Sign in to use your wallet."))
        return try {
            val row = walletDao.getByUser(uid)
            val balance = row?.balanceGhs ?: run {
                val now = System.currentTimeMillis()
                val initial = WalletEntity(uid, DEMO_START_GHS, now)
                walletDao.upsert(initial)
                DEMO_START_GHS
            }
            Result.success(roundMoney(balance))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun topUp(amountGhs: Double): Result<Double> {
        val uid = currentUserId() ?: return Result.failure(Exception("Sign in to add funds."))
        if (amountGhs <= 0) return Result.failure(Exception("Enter a positive amount."))
        if (amountGhs > 50_000) return Result.failure(Exception("Demo limit: GHS 50,000 per top-up."))
        return try {
            val now = System.currentTimeMillis()
            val existing = walletDao.getByUser(uid)
                ?: WalletEntity(uid, DEMO_START_GHS, now).also { walletDao.upsert(it) }
            val next = roundMoney(existing.balanceGhs + amountGhs)
            walletDao.upsert(existing.copy(balanceGhs = next, updatedAtEpochMs = now))
            Result.success(next)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun tryDebit(amountGhs: Double): Result<Unit> {
        val uid = currentUserId() ?: return Result.failure(Exception("Sign in to pay from your wallet."))
        if (amountGhs <= 0) return Result.success(Unit)
        return try {
            val now = System.currentTimeMillis()
            val existing = walletDao.getByUser(uid)
                ?: WalletEntity(uid, DEMO_START_GHS, now).also { walletDao.upsert(it) }
            val bal = roundMoney(existing.balanceGhs)
            val need = roundMoney(amountGhs)
            if (bal < need) {
                return Result.failure(Exception("Insufficient balance. Add funds in Profile (wallet)."))
            }
            walletDao.upsert(
                existing.copy(balanceGhs = roundMoney(bal - need), updatedAtEpochMs = now)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
