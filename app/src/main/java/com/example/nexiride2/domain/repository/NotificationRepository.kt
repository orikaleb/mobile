package com.example.nexiride2.domain.repository

import com.example.nexiride2.domain.model.AppNotification

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<AppNotification>>
    suspend fun markAsRead(notificationId: String): Result<Boolean>
    suspend fun markAllAsRead(): Result<Boolean>
    fun getUnreadCount(): Int
}
