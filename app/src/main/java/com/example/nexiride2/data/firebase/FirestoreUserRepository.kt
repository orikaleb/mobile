package com.example.nexiride2.data.firebase

import com.example.nexiride2.domain.model.PaymentMethod
import com.example.nexiride2.domain.model.PaymentType
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : UserRepository {

    private fun currentId(): String? = authRepository.getCurrentUser()?.id?.takeIf { it.isNotBlank() }

    private fun userDoc(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid)

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

    override suspend fun getUser(): Result<User> {
        val base = authRepository.getCurrentUser() ?: return Result.failure(Exception("Not signed in"))
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            val snap = userDoc(uid).get().await()
            if (!snap.exists()) {
                return Result.success(base)
            }
            Result.success(
                base.copy(
                    name = snap.getString("fullName")?.takeIf { it.isNotBlank() } ?: base.name,
                    phone = snap.getString("phone")?.takeIf { it.isNotBlank() } ?: base.phone,
                    profilePhotoUrl = snap.getString("avatarUrl")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(name: String, email: String, phone: String): Result<User> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            userDoc(uid).set(
                mapOf(
                    "fullName" to name,
                    "phone" to phone,
                    "updatedAt" to isoNow()
                ),
                SetOptions.merge()
            ).await()
            getUser()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePhoto(photoUri: String): Result<User> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            userDoc(uid).set(
                mapOf(
                    "avatarUrl" to photoUri,
                    "updatedAt" to isoNow()
                ),
                SetOptions.merge()
            ).await()
            getUser()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSavedPaymentMethods(): Result<List<PaymentMethod>> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            val snap = userDoc(uid).collection(FirestorePaths.PAYMENT_METHODS).get().await()
            val list = snap.documents.map { d ->
                val type = PaymentType.entries.find { it.name == d.getString("type") } ?: PaymentType.CASH_AT_STATION
                PaymentMethod(
                    id = d.id,
                    type = type,
                    name = d.getString("name").orEmpty(),
                    details = d.getString("details").orEmpty(),
                    isDefault = d.getBoolean("isDefault") ?: false
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addPaymentMethod(paymentMethod: PaymentMethod): Result<Boolean> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            val ref = userDoc(uid).collection(FirestorePaths.PAYMENT_METHODS).document()
            ref.set(
                mapOf(
                    "type" to paymentMethod.type.name,
                    "name" to paymentMethod.name,
                    "details" to paymentMethod.details,
                    "isDefault" to paymentMethod.isDefault
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removePaymentMethod(paymentMethodId: String): Result<Boolean> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            userDoc(uid).collection(FirestorePaths.PAYMENT_METHODS).document(paymentMethodId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
