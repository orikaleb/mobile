package com.example.nexiride2.data.firebase

import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private fun FirebaseUser.toUser(): User =
        User(
            id = uid,
            name = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore("@") ?: "Traveler",
            email = email.orEmpty(),
            phone = ""
        )

    override suspend fun login(email: String, password: String): Result<User> =
        runCatching {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            auth.currentUser!!.toUser()
        }

    override suspend fun signUp(name: String, email: String, phone: String, password: String): Result<User> =
        runCatching {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user!!
            runCatching {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
            }
            // Ensure Security Rules see request.auth before any Firestore write.
            runCatching { user.getIdToken(true).await() }
            // Best-effort profile write: if Firestore rules block it, sign-up still succeeds
            // and FirestoreUserRepository will upsert the profile on first read.
            runCatching {
                firestore.collection(FirestorePaths.USERS).document(user.uid)
                    .set(
                        mapOf(
                            "fullName" to name,
                            "phone" to phone
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }
            user.toUser().copy(name = name, phone = phone)
        }

    override suspend fun forgotPassword(email: String): Result<Boolean> =
        runCatching {
            auth.sendPasswordResetEmail(email.trim()).await()
            true
        }

    override suspend fun verifyOtp(email: String, otp: String): Result<Boolean> =
        Result.failure(Exception("Use the reset link from your email, or sign in with password."))

    override suspend fun resetPassword(email: String, newPassword: String): Result<Boolean> =
        runCatching {
            auth.currentUser ?: error("Sign in with the reset link first, then update password from the app.")
            auth.currentUser!!.updatePassword(newPassword).await()
            true
        }

    override suspend fun logout() {
        auth.signOut()
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override fun getCurrentUser(): User? = auth.currentUser?.toUser()
}
