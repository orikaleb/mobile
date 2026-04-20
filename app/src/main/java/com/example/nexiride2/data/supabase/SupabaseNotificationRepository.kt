package com.example.nexiride2.data.supabase

import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.supabase.dto.NotificationPatchDto
import com.example.nexiride2.domain.model.AppNotification
import com.example.nexiride2.domain.model.NotificationType
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.NotificationRepository
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseNotificationRepository @Inject constructor(
    private val api: SupabasePostgrestApi,
    private val authRepository: AuthRepository
) : NotificationRepository {

    private val unreadCache = AtomicInteger(0)

    private fun configured(): Boolean = BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun currentId(): String? = authRepository.getCurrentUser()?.id?.takeIf { it.isNotBlank() }

    private fun mapType(raw: String): NotificationType =
        NotificationType.entries.find { it.name == raw } ?: NotificationType.GENERAL

    override suspend fun getNotifications(): Result<List<AppNotification>> {
        val uid = currentId() ?: return Result.success(emptyList())
        if (!configured()) return Result.success(emptyList())
        return try {
            val list = api.notificationsByUser("eq.$uid")
                .sortedByDescending { it.createdAt }
                .map { row ->
                    AppNotification(
                        id = row.id,
                        title = row.title,
                        message = row.message,
                        type = mapType(row.type),
                        timestamp = row.createdAt.take(19).replace('T', ' '),
                        isRead = row.isRead,
                        bookingId = row.bookingId
                    )
                }
            unreadCache.set(list.count { !it.isRead })
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Boolean> {
        if (!configured()) return Result.failure(Exception("Supabase is not configured."))
        return try {
            api.patchNotification("eq.$notificationId", NotificationPatchDto(isRead = true))
            unreadCache.updateAndGet { (it - 1).coerceAtLeast(0) }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(): Result<Boolean> {
        val uid = currentId() ?: return Result.success(true)
        if (!configured()) return Result.success(true)
        return try {
            val unread = api.notificationsByUser("eq.$uid").filter { !it.isRead }
            unread.forEach { api.patchNotification("eq.${it.id}", NotificationPatchDto(isRead = true)) }
            unreadCache.set(0)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUnreadCount(): Int = unreadCache.get()
}
