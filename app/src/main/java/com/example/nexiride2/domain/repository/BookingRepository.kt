package com.example.nexiride2.domain.repository

import com.example.nexiride2.domain.model.Booking
import com.example.nexiride2.domain.model.Passenger
import com.example.nexiride2.domain.model.BaggageInfo
import com.example.nexiride2.domain.model.Seat

interface BookingRepository {
    suspend fun createBooking(routeId: String, seats: List<Seat>, passengers: List<Passenger>, baggage: BaggageInfo?, paymentMethod: String): Result<Booking>
    suspend fun getUpcomingBookings(): Result<List<Booking>>
    suspend fun getPastBookings(): Result<List<Booking>>
    suspend fun getCancelledBookings(): Result<List<Booking>>
    suspend fun getBookingById(bookingId: String): Result<Booking>
    suspend fun cancelBooking(bookingId: String): Result<Booking>
    suspend fun getRecentRouteIds(): List<String>
}
