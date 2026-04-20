package com.example.nexiride2.data.supabase

import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.supabase.dto.PaymentMethodInsertDto
import com.example.nexiride2.data.supabase.dto.PaymentMethodRowDto
import com.example.nexiride2.data.supabase.dto.ProfilePatchDto
import com.example.nexiride2.domain.model.PaymentMethod
import com.example.nexiride2.domain.model.PaymentType
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.UserRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseUserRepository @Inject constructor(
    private val api: SupabasePostgrestApi,
    private val authRepository: AuthRepository
) : UserRepository {

    private fun configured(): Boolean = BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun currentId(): String? = authRepository.getCurrentUser()?.id?.takeIf { it.isNotBlank() }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

    private fun PaymentMethodRowDto.toDomain(): PaymentMethod {
        val type = PaymentType.entries.find { it.name == this.type } ?: PaymentType.CASH_AT_STATION
        return PaymentMethod(id = id, type = type, name = name, details = details, isDefault = isDefault)
    }

    override suspend fun getUser(): Result<User> {
        val base = authRepository.getCurrentUser() ?: return Result.failure(Exception("Not signed in"))
        if (!configured()) return Result.success(base)
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            val profile = api.profileById("eq.$uid").firstOrNull()
            Result.success(
                base.copy(
                    name = profile?.fullName?.takeIf { it.isNotBlank() } ?: base.name,
                    phone = profile?.phone?.takeIf { it.isNotBlank() } ?: base.phone,
                    profilePhotoUrl = profile?.avatarUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(name: String, email: String, phone: String): Result<User> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        if (!configured()) return Result.failure(Exception("Supabase is not configured."))
        return try {
            val existing = api.profileById("eq.$uid").firstOrNull()
            api.patchProfile(
                idEq = "eq.$uid",
                body = ProfilePatchDto(
                    fullName = name,
                    phone = phone,
                    avatarUrl = existing?.avatarUrl,
                    updatedAt = isoNow()
                )
            )
            getUser()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePhoto(photoUri: String): Result<User> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        if (!configured()) return Result.failure(Exception("Supabase is not configured."))
        return try {
            val existing = api.profileById("eq.$uid").firstOrNull()
            api.patchProfile(
                idEq = "eq.$uid",
                body = ProfilePatchDto(
                    fullName = existing?.fullName,
                    phone = existing?.phone,
                    avatarUrl = photoUri,
                    updatedAt = isoNow()
                )
            )
            getUser()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSavedPaymentMethods(): Result<List<PaymentMethod>> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        if (!configured()) return Result.success(emptyList())
        return try {
            Result.success(api.paymentMethodsByUser("eq.$uid").map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addPaymentMethod(paymentMethod: PaymentMethod): Result<Boolean> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        if (!configured()) return Result.failure(Exception("Supabase is not configured."))
        return try {
            api.insertPaymentMethod(
                PaymentMethodInsertDto(
                    userId = uid,
                    type = paymentMethod.type.name,
                    name = paymentMethod.name,
                    details = paymentMethod.details,
                    isDefault = paymentMethod.isDefault
                )
            )
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removePaymentMethod(paymentMethodId: String): Result<Boolean> {
        if (!configured()) return Result.failure(Exception("Supabase is not configured."))
        return try {
            api.deletePaymentMethod("eq.$paymentMethodId")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
