package com.example.nexiride2.domain.model

data class Booking(
    val id: String,
    val referenceCode: String,
    val route: Route,
    val seats: List<Seat>,
    val passengers: List<Passenger>,
    val status: BookingStatus,
    val totalPrice: Double,
    val currency: String = "GHS",
    val paymentMethod: String,
    val bookingDate: String,
    val qrCodeData: String = "",
    val baggage: BaggageInfo? = null
)

data class Passenger(val name: String, val phone: String = "", val seatNumber: String = "")
data class BaggageInfo(val numberOfBags: Int = 0, val totalWeight: Double = 0.0)
enum class BookingStatus { CONFIRMED, PENDING, CANCELLED, COMPLETED }
