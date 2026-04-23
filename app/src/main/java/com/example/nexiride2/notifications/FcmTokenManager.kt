package com.example.nexiride2.notifications

import com.example.nexiride2.data.firebase.FirestorePaths
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists this device's FCM registration token under the current user's
 * Firestore profile so the `pushBroadcastNotification` Cloud Function can
 * target the device when an admin posts a new `notifications/{id}` doc.
 *
 * The token is stored in `users/{uid}.fcmTokens` (array) — drivers have the
 * same field on `drivers/{uid}` so broadcasts can reach bus drivers too.
 * Tokens are added on login and removed on logout to avoid cross-account leaks.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fetches the current FCM token and writes it to the signed-in user's
     * (and driver's, if applicable) Firestore profile. Safe to call any time —
     * no-ops if there is no signed-in user.
     */
    fun registerCurrentDevice() {
        scope.launch { runCatching { registerBlocking() } }
    }

    /**
     * Called from [NexiRideMessagingService.onNewToken] — writes the new token
     * eagerly without needing to pull it again.
     */
    fun saveTokenAsync(token: String) {
        scope.launch { runCatching { persist(token) } }
    }

    /**
     * Removes this device's token from the signed-in user's profile so the
     * account doesn't keep receiving pushes after logout on this device.
     */
    fun unregisterCurrentDevice() {
        scope.launch {
            runCatching {
                val token = messaging.token.await() ?: return@runCatching
                removeToken(token)
            }
        }
    }

    private suspend fun registerBlocking() {
        val token = messaging.token.await() ?: return
        persist(token)
    }

    private suspend fun persist(token: String) {
        val uid = auth.currentUser?.uid ?: return
        val payload = mapOf("fcmTokens" to FieldValue.arrayUnion(token))
        // Write to both collections so broadcasts reach passengers AND drivers.
        // SetOptions.merge lets this coexist with existing profile fields.
        runCatching {
            db.collection(FirestorePaths.USERS).document(uid)
                .set(payload, SetOptions.merge()).await()
        }
        runCatching {
            db.collection(FirestorePaths.DRIVERS).document(uid)
                .set(payload, SetOptions.merge()).await()
        }
    }

    private suspend fun removeToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        val payload = mapOf("fcmTokens" to FieldValue.arrayRemove(token))
        runCatching {
            db.collection(FirestorePaths.USERS).document(uid)
                .set(payload, SetOptions.merge()).await()
        }
        runCatching {
            db.collection(FirestorePaths.DRIVERS).document(uid)
                .set(payload, SetOptions.merge()).await()
        }
    }
}
