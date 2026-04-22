package com.example.nexiride2.domain.usecase

import com.example.nexiride2.domain.model.*
import com.example.nexiride2.domain.repository.BookingRepository

class CreateBookingUseCase(private val bookingRepository: BookingRepository) {
    suspend operator fun invoke(routeId: String, seats: List<Seat>, passengers: List<Passenger>, baggage: BaggageInfo?, paymentMethod: String): Result<Booking> {
        if (seats.isEmpty()) return Result.failure(IllegalArgumentException("Select at least one seat"))
        if (passengers.isEmpty()) return Result.failure(IllegalArgumentException("Add at least one passenger"))
        if (seats.size != passengers.size) return Result.failure(IllegalArgumentException("Each seat needs a passenger"))
        return bookingRepository.createBooking(routeId, seats, passengers, baggage, paymentMethod)
    }
}

class GetBookingsUseCase(private val bookingRepository: BookingRepository) {
    /** Single remote fetch, then filter locally for the three tabs. */
    suspend fun getGroupedBookings() = bookingRepository.getAllBookingsForCurrentUser().map { all ->
        Triple(
            all.filter { it.status == BookingStatus.CONFIRMED },
            all.filter { it.status == BookingStatus.COMPLETED },
            all.filter { it.status == BookingStatus.CANCELLED }
        )
    }

    suspend fun getUpcoming() = bookingRepository.getUpcomingBookings()
    suspend fun getPast() = bookingRepository.getPastBookings()
    suspend fun getCancelled() = bookingRepository.getCancelledBookings()
}

class CancelBookingUseCase(private val bookingRepository: BookingRepository) {
    suspend operator fun invoke(bookingId: String) = bookingRepository.cancelBooking(bookingId)
}
