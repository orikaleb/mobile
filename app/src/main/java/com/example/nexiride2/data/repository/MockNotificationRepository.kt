package com.example.nexiride2.data.repository

import com.example.nexiride2.data.local.MockData
import com.example.nexiride2.domain.model.AppNotification
import com.example.nexiride2.domain.repository.NotificationRepository
import kotlinx.coroutines.delay

class MockNotificationRepository : NotificationRepository {
    override suspend fun getNotifications(): Result<List<AppNotification>> {
        delay(300); return Result.success(MockData.notifications.toList())
    }
    override suspend fun markAsRead(notificationId: String): Result<Boolean> {
        val idx = MockData.notifications.indexOfFirst { it.id == notificationId }
        if (idx != -1) MockData.notifications[idx] = MockData.notifications[idx].copy(isRead = true)
        return Result.success(true)
    }
    override suspend fun markAllAsRead(): Result<Boolean> {
        MockData.notifications.forEachIndexed { i, n -> MockData.notifications[i] = n.copy(isRead = true) }
        return Result.success(true)
    }
    override fun getUnreadCount() = MockData.notifications.count { !it.isRead }
}
