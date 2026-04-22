package com.example.nexiride2.data.firebase

import android.net.Uri
import com.example.nexiride2.domain.model.PaymentMethod
import com.example.nexiride2.domain.model.PaymentType
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
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
    private val storage: FirebaseStorage,
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
                // First read for this user: create a minimal profile doc so the admin
                // can see them in /users. Best-effort — failures (rules / offline) are ignored.
                runCatching {
                    userDoc(uid).set(
                        mapOf(
                            "fullName" to base.name,
                            "phone" to base.phone,
                            "email" to base.email,
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "updatedAt" to isoNow()
                        ),
                        SetOptions.merge()
                    ).await()
                }
                return Result.success(base)
            }
            // Backfill email if an older doc predates the field.
            if (snap.getString("email").isNullOrBlank() && base.email.isNotBlank()) {
                runCatching {
                    userDoc(uid).set(mapOf("email" to base.email), SetOptions.merge()).await()
                }
            }
            Result.success(
                base.copy(
                    name = snap.getString("fullName")?.takeIf { it.isNotBlank() } ?: base.name,
                    phone = snap.getString("phone")?.takeIf { it.isNotBlank() } ?: base.phone,
                    profilePhotoUrl = snap.getString("avatarUrl")
                )
            )
        } catch (_: Exception) {
            // Profile doc unreadable (rules / offline): fall back to Auth-provided user.
            Result.success(base)
        }
    }

    override suspend fun updateUser(name: String, email: String, phone: String): Result<User> {
        val uid = currentId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            userDoc(uid).set(
                mapOf(
                    "fullName" to name,
                    "phone" to phone,
                    // Firebase Auth email is the source of truth; persist it here
                    // so the admin listing always has a populated email column.
                    "email" to (email.ifBlank { authRepository.getCurrentUser()?.email.orEmpty() }),
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
            val downloadUrl = when {
                photoUri.startsWith("http://", ignoreCase = true) ||
                    photoUri.startsWith("https://", ignoreCase = true) -> photoUri
                else -> {
                    val uri = Uri.parse(photoUri)
                    val ref = storage.reference.child("avatars/$uid/${System.currentTimeMillis()}.jpg")
                    ref.putFile(uri).await()
                    ref.downloadUrl.await().toString()
                }
            }
            userDoc(uid).set(
                mapOf(
                    "avatarUrl" to downloadUrl,
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
