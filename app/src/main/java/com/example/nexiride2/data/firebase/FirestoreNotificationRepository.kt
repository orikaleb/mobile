package com.example.nexiride2.data.firebase

import com.example.nexiride2.domain.model.AppNotification
import com.example.nexiride2.domain.model.NotificationType
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNotificationRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val authRepository: AuthRepository
) : NotificationRepository {

    private val unreadCache = AtomicInteger(0)

    private fun currentId(): String? = authRepository.getCurrentUser()?.id?.takeIf { it.isNotBlank() }

    private fun mapType(raw: String): NotificationType =
        NotificationType.entries.find { it.name == raw } ?: NotificationType.GENERAL

    private fun formatTimestamp(value: Any?): String {
        return when (value) {
            is Timestamp -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(value.toDate())
            is String -> value.take(19).replace('T', ' ')
            else -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
        }
    }

    override suspend fun getNotifications(): Result<List<AppNotification>> {
        val uid = currentId() ?: return Result.success(emptyList())
        return try {
            val snap = db.collection(FirestorePaths.NOTIFICATIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val list = snap.documents
                .map { d ->
                    AppNotification(
                        id = d.id,
                        title = d.getString("title").orEmpty(),
                        message = d.getString("message").orEmpty(),
                        type = mapType(d.getString("type").orEmpty()),
                        timestamp = formatTimestamp(d.get("createdAt")),
                        isRead = d.getBoolean("isRead") ?: false,
                        bookingId = d.getString("bookingId")
                    )
                }
                .sortedByDescending { it.timestamp }
            unreadCache.set(list.count { !it.isRead })
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Boolean> {
        return try {
            db.collection(FirestorePaths.NOTIFICATIONS).document(notificationId)
                .update("isRead", true)
                .await()
            unreadCache.updateAndGet { (it - 1).coerceAtLeast(0) }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(): Result<Boolean> {
        val uid = currentId() ?: return Result.success(true)
        return try {
            val snap = db.collection(FirestorePaths.NOTIFICATIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val batch = db.batch()
            snap.documents.filter { it.getBoolean("isRead") != true }.forEach { d ->
                batch.update(d.reference, "isRead", true)
            }
            batch.commit().await()
            unreadCache.set(0)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUnreadCount(): Int = unreadCache.get()
}
