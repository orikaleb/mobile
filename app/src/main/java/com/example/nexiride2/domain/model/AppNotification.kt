package com.example.nexiride2.domain.model

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: String,
    val isRead: Boolean = false,
    val bookingId: String? = null
)

enum class NotificationType {
    BOOKING_CONFIRMATION, TRIP_REMINDER, DELAY, CANCELLATION, REFUND, PROMO, GENERAL
}
