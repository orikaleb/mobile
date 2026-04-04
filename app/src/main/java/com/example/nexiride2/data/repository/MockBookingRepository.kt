package com.example.nexiride2.data.repository

import com.example.nexiride2.data.local.MockData
import com.example.nexiride2.domain.model.*
import com.example.nexiride2.domain.repository.BookingRepository
import kotlinx.coroutines.delay
import java.util.UUID

class MockBookingRepository : BookingRepository {
    override suspend fun createBooking(routeId: String, seats: List<Seat>, passengers: List<Passenger>, baggage: BaggageInfo?, paymentMethod: String): Result<Booking> {
        delay(1200)
        val route = MockData.routes.find { it.id == routeId } ?: return Result.failure(Exception("Route not found"))
        val ref = "NXR-${UUID.randomUUID().toString().take(8).uppercase()}"
        val booking = Booking(UUID.randomUUID().toString(), ref, route, seats, passengers, BookingStatus.CONFIRMED,
            seats.size * route.price, "GHS", paymentMethod, "2025-04-15", ref, baggage)
        MockData.bookings.add(0, booking)
        MockData.notifications.add(0, AppNotification(UUID.randomUUID().toString(), "Booking Confirmed!",
            "Your booking $ref for ${route.origin} → ${route.destination} has been confirmed.",
            NotificationType.BOOKING_CONFIRMATION, "Just now", false, booking.id))
        return Result.success(booking)
    }

    override suspend fun getUpcomingBookings(): Result<List<Booking>> {
        delay(300); return Result.success(MockData.bookings.filter { it.status == BookingStatus.CONFIRMED })
    }
    override suspend fun getPastBookings(): Result<List<Booking>> {
        delay(300); return Result.success(MockData.bookings.filter { it.status == BookingStatus.COMPLETED })
    }
    override suspend fun getCancelledBookings(): Result<List<Booking>> {
        delay(300); return Result.success(MockData.bookings.filter { it.status == BookingStatus.CANCELLED })
    }
    override suspend fun getBookingById(bookingId: String): Result<Booking> {
        delay(200); return MockData.bookings.find { it.id == bookingId }?.let { Result.success(it) }
            ?: Result.failure(Exception("Booking not found"))
    }
    override suspend fun cancelBooking(bookingId: String): Result<Booking> {
        delay(800)
        val idx = MockData.bookings.indexOfFirst { it.id == bookingId }
        if (idx == -1) return Result.failure(Exception("Booking not found"))
        val cancelled = MockData.bookings[idx].copy(status = BookingStatus.CANCELLED)
        MockData.bookings[idx] = cancelled
        return Result.success(cancelled)
    }
    override suspend fun getRecentRouteIds() = listOf("r1", "r4", "r3")
}
