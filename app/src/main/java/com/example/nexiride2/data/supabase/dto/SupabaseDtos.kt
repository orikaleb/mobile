package com.example.nexiride2.data.supabase.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteRowDto(
    val id: String,
    val origin: String,
    val destination: String,
    val date: String,
    @SerializedName("available_seats") @SerialName("available_seats") val availableSeats: Int,
    @SerializedName("route_json") @SerialName("route_json") val routeJson: String
)

@Serializable
data class BookingRowDto(
    val id: String,
    @SerializedName("user_id") @SerialName("user_id") val userId: String,
    val status: String,
    @SerializedName("route_id") @SerialName("route_id") val routeId: String,
    @SerializedName("booking_json") @SerialName("booking_json") val bookingJson: String
)

@Serializable
data class CityRowDto(
    val name: String
)

@Serializable
data class BookingPatchDto(
    val status: String,
    @SerializedName("booking_json") @SerialName("booking_json") val bookingJson: String
)

@Serializable
data class RouteSeatRowDto(
    @SerializedName("route_id") @SerialName("route_id") val routeId: String,
    @SerializedName("seat_number") @SerialName("seat_number") val seatNumber: String,
    @SerializedName("row_idx") @SerialName("row_idx") val rowIdx: Int,
    @SerializedName("col_idx") @SerialName("col_idx") val colIdx: Int,
    val status: String,
    @SerializedName("booking_id") @SerialName("booking_id") val bookingId: String? = null
)

@Serializable
data class RouteSeatInsertDto(
    @SerializedName("route_id") @SerialName("route_id") val routeId: String,
    @SerializedName("seat_number") @SerialName("seat_number") val seatNumber: String,
    @SerializedName("row_idx") @SerialName("row_idx") val rowIdx: Int,
    @SerializedName("col_idx") @SerialName("col_idx") val colIdx: Int,
    val status: String = "AVAILABLE"
)

@Serializable
data class RouteSeatPatchDto(
    val status: String,
    @SerializedName("booking_id") @SerialName("booking_id") val bookingId: String? = null
)

@Serializable
data class ProfileRowDto(
    val id: String,
    @SerializedName("full_name") @SerialName("full_name") val fullName: String?,
    val phone: String?,
    @SerializedName("avatar_url") @SerialName("avatar_url") val avatarUrl: String?
)

@Serializable
data class ProfilePatchDto(
    @SerializedName("full_name") @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    @SerializedName("avatar_url") @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("updated_at") @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class NotificationRowDto(
    val id: String,
    @SerializedName("user_id") @SerialName("user_id") val userId: String,
    val title: String,
    val message: String,
    val type: String,
    @SerializedName("booking_id") @SerialName("booking_id") val bookingId: String? = null,
    @SerializedName("is_read") @SerialName("is_read") val isRead: Boolean,
    @SerializedName("created_at") @SerialName("created_at") val createdAt: String
)

@Serializable
data class NotificationPatchDto(
    @SerializedName("is_read") @SerialName("is_read") val isRead: Boolean
)

@Serializable
data class NotificationInsertDto(
    @SerializedName("user_id") @SerialName("user_id") val userId: String,
    val title: String,
    val message: String,
    val type: String,
    @SerializedName("booking_id") @SerialName("booking_id") val bookingId: String? = null
)

@Serializable
data class PaymentMethodRowDto(
    val id: String,
    @SerializedName("user_id") @SerialName("user_id") val userId: String,
    val type: String,
    val name: String,
    val details: String,
    @SerializedName("is_default") @SerialName("is_default") val isDefault: Boolean
)

@Serializable
data class PaymentMethodInsertDto(
    @SerializedName("user_id") @SerialName("user_id") val userId: String,
    val type: String,
    val name: String,
    val details: String,
    @SerializedName("is_default") @SerialName("is_default") val isDefault: Boolean = false
)

@Serializable
data class PaymentTransactionInsertDto(
    @SerializedName("user_id") @SerialName("user_id") val userId: String,
    @SerializedName("booking_id") @SerialName("booking_id") val bookingId: String,
    val amount: Double,
    val currency: String = "GHS",
    val method: String,
    val status: String = "CAPTURED"
)
