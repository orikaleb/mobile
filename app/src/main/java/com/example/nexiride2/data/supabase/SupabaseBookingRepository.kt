package com.example.nexiride2.data.supabase

import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.supabase.dto.BookingPatchDto
import com.example.nexiride2.data.supabase.dto.BookingRowDto
import com.example.nexiride2.data.supabase.dto.NotificationInsertDto
import com.example.nexiride2.data.supabase.dto.RouteSeatPatchDto
import com.example.nexiride2.domain.model.BaggageInfo
import com.example.nexiride2.domain.model.Booking
import com.example.nexiride2.domain.model.BookingStatus
import com.example.nexiride2.domain.model.Passenger
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.BookingRepository
import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseBookingRepository @Inject constructor(
    private val api: SupabasePostgrestApi,
    private val gson: Gson,
    private val authRepository: AuthRepository,
    private val supabaseBusRepository: SupabaseBusRepository
) : BookingRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun currentUserId(): String? = authRepository.getCurrentUser()?.id

    private fun configured(): Boolean = BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun parseBooking(row: BookingRowDto): Booking? =
        runCatching { gson.fromJson(row.bookingJson, Booking::class.java) }.getOrNull()

    private suspend fun allBookingsForUser(): List<Booking> {
        val uid = currentUserId() ?: return emptyList()
        if (!configured()) return emptyList()
        return try {
            api.bookingsByUser("eq.$uid").mapNotNull { parseBooking(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun createBooking(
        routeId: String,
        seats: List<Seat>,
        passengers: List<Passenger>,
        baggage: BaggageInfo?,
        paymentMethod: String
    ): Result<Booking> {
        val uid = currentUserId() ?: return Result.failure(Exception("Sign in required to save a booking."))
        if (!configured()) {
            return Result.failure(Exception("Supabase is not configured (local.properties)."))
        }
        return try {
            delay(200)
            val route = supabaseBusRepository.getRouteById(routeId).getOrElse {
                return Result.failure(Exception("Route not found"))
            }
            val seatRows = api.routeSeatsByRoute("eq.$routeId").associateBy { it.seatNumber }
            for (s in seats) {
                val row = seatRows[s.number] ?: return Result.failure(Exception("Seat ${s.number} not found for this route."))
                if (row.status.uppercase() != "AVAILABLE" || row.bookingId != null) {
                    return Result.failure(Exception("Seat ${s.number} is no longer available."))
                }
            }
            val ref = "NXR-${UUID.randomUUID().toString().take(8).uppercase()}"
            val bookingId = UUID.randomUUID().toString()
            val booking = Booking(
                id = bookingId,
                referenceCode = ref,
                route = route,
                seats = seats,
                passengers = passengers,
                status = BookingStatus.CONFIRMED,
                totalPrice = seats.size * route.price,
                currency = "GHS",
                paymentMethod = paymentMethod,
                bookingDate = dateFormat.format(Date()),
                qrCodeData = ref,
                baggage = baggage
            )
            val row = BookingRowDto(
                id = bookingId,
                userId = uid,
                status = booking.status.name,
                routeId = routeId,
                bookingJson = gson.toJson(booking)
            )
            api.insertBooking(row)
            for (s in seats) {
                api.patchRouteSeat(
                    routeIdEq = "eq.$routeId",
                    seatNumberEq = "eq.${s.number}",
                    body = RouteSeatPatchDto(status = "BOOKED", bookingId = bookingId)
                )
            }
            runCatching {
                api.insertNotification(
                    NotificationInsertDto(
                        userId = uid,
                        title = "Booking confirmed",
                        message = "Your trip ${route.origin} → ${route.destination} is confirmed. Ref $ref.",
                        type = "BOOKING_CONFIRMATION",
                        bookingId = bookingId
                    )
                )
            }
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUpcomingBookings(): Result<List<Booking>> {
        return try {
            delay(150)
            Result.success(allBookingsForUser().filter { it.status == BookingStatus.CONFIRMED })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPastBookings(): Result<List<Booking>> {
        return try {
            delay(150)
            Result.success(allBookingsForUser().filter { it.status == BookingStatus.COMPLETED })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCancelledBookings(): Result<List<Booking>> {
        return try {
            delay(150)
            Result.success(allBookingsForUser().filter { it.status == BookingStatus.CANCELLED })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBookingById(bookingId: String): Result<Booking> {
        if (!configured()) {
            return Result.failure(Exception("Supabase is not configured."))
        }
        return try {
            delay(100)
            val row = api.bookingById("eq.$bookingId").firstOrNull()
                ?: return Result.failure(Exception("Booking not found"))
            parseBooking(row)?.let { Result.success(it) }
                ?: Result.failure(Exception("Booking not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelBooking(bookingId: String): Result<Booking> {
        if (!configured()) {
            return Result.failure(Exception("Supabase is not configured."))
        }
        return try {
            delay(300)
            val existing = api.bookingById("eq.$bookingId").firstOrNull()
                ?.let { parseBooking(it) }
                ?: return Result.failure(Exception("Booking not found"))
            val held = api.routeSeatsByBooking("eq.$bookingId")
            for (sr in held) {
                api.patchRouteSeat(
                    routeIdEq = "eq.${sr.routeId}",
                    seatNumberEq = "eq.${sr.seatNumber}",
                    body = RouteSeatPatchDto(status = "AVAILABLE", bookingId = null)
                )
            }
            val cancelled = existing.copy(status = BookingStatus.CANCELLED)
            val updated = api.patchBooking(
                idEq = "eq.$bookingId",
                body = BookingPatchDto(
                    status = BookingStatus.CANCELLED.name,
                    bookingJson = gson.toJson(cancelled)
                )
            ).firstOrNull()?.let { parseBooking(it) }
            Result.success(updated ?: cancelled)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentRouteIds(): List<String> {
        return try {
            allBookingsForUser()
                .sortedByDescending { it.bookingDate }
                .take(3)
                .map { it.route.id }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
