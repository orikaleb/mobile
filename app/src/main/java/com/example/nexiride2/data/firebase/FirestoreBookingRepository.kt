package com.example.nexiride2.data.firebase

import com.example.nexiride2.domain.model.BaggageInfo
import com.example.nexiride2.domain.model.Booking
import com.example.nexiride2.domain.model.BookingStatus
import com.example.nexiride2.domain.model.Passenger
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.BookingRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBookingRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val gson: Gson,
    private val authRepository: AuthRepository,
    private val busRepository: FirestoreBusRepository
) : BookingRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun currentUserId(): String? = authRepository.getCurrentUser()?.id

    private fun parseBooking(data: Map<String, Any>?): Booking? {
        val json = data?.get("bookingJson") as? String ?: return null
        return runCatching { gson.fromJson(json, Booking::class.java) }.getOrNull()
    }

    private suspend fun allBookingsForUser(): Result<List<Booking>> {
        val uid = currentUserId() ?: return Result.success(emptyList())
        return runCatching {
            val snap = db.collection(FirestorePaths.BOOKINGS)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.documents.mapNotNull { parseBooking(it.data) }
        }
    }

    override suspend fun getAllBookingsForCurrentUser(): Result<List<Booking>> {
        delay(150)
        return allBookingsForUser()
    }

    override suspend fun createBooking(
        routeId: String,
        seats: List<Seat>,
        passengers: List<Passenger>,
        baggage: BaggageInfo?,
        paymentMethod: String
    ): Result<Booking> {
        val uid = currentUserId() ?: return Result.failure(Exception("Sign in required to save a booking."))
        return try {
            delay(200)
            val route = busRepository.getRouteById(routeId).getOrElse {
                return Result.failure(Exception("Route not found"))
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
            val bookingJson = gson.toJson(booking)
            val bookingRef = db.collection(FirestorePaths.BOOKINGS).document(bookingId)
            db.runTransaction { tx ->
                for (s in seats) {
                    val seatRef = db.collection(FirestorePaths.ROUTES).document(routeId)
                        .collection(FirestorePaths.SEATS).document(s.number)
                    val snap = tx.get(seatRef)
                    if (!snap.exists()) {
                        val n = s.number.toIntOrNull() ?: 1
                        tx.set(
                            seatRef,
                            mapOf(
                                "status" to "AVAILABLE",
                                "rowIdx" to ((n - 1) / 4 + 1).toLong(),
                                "colIdx" to ((n - 1) % 4 + 1).toLong()
                            )
                        )
                    } else {
                        val status = (snap.getString("status") ?: "AVAILABLE").uppercase(Locale.US)
                        val heldBy = snap.getString("bookingId")
                        if (status != "AVAILABLE" || !heldBy.isNullOrBlank()) {
                            throw IllegalStateException("Seat ${s.number} is no longer available.")
                        }
                    }
                }
                tx.set(
                    bookingRef,
                    mapOf(
                        "id" to bookingId,
                        "userId" to uid,
                        "status" to booking.status.name,
                        "routeId" to routeId,
                        "bookingJson" to bookingJson
                    )
                )
                for (s in seats) {
                    val seatRef = db.collection(FirestorePaths.ROUTES).document(routeId)
                        .collection(FirestorePaths.SEATS).document(s.number)
                    tx.update(seatRef, mapOf("status" to "BOOKED", "bookingId" to bookingId))
                }
                null
            }.await()
            runCatching {
                val notifId = UUID.randomUUID().toString()
                db.collection(FirestorePaths.NOTIFICATIONS).document(notifId).set(
                    mapOf(
                        "id" to notifId,
                        "userId" to uid,
                        "title" to "Booking confirmed",
                        "message" to "Your trip ${route.origin} → ${route.destination} is confirmed. Ref $ref.",
                        "type" to "BOOKING_CONFIRMATION",
                        "bookingId" to bookingId,
                        "isRead" to false,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                ).await()
            }
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUpcomingBookings(): Result<List<Booking>> {
        delay(150)
        return allBookingsForUser().map { list -> list.filter { it.status == BookingStatus.CONFIRMED } }
    }

    override suspend fun getPastBookings(): Result<List<Booking>> {
        delay(150)
        return allBookingsForUser().map { list -> list.filter { it.status == BookingStatus.COMPLETED } }
    }

    override suspend fun getCancelledBookings(): Result<List<Booking>> {
        delay(150)
        return allBookingsForUser().map { list -> list.filter { it.status == BookingStatus.CANCELLED } }
    }

    override suspend fun getBookingById(bookingId: String): Result<Booking> {
        return try {
            delay(100)
            val doc = db.collection(FirestorePaths.BOOKINGS).document(bookingId).get().await()
            val booking = parseBooking(doc.data) ?: return Result.failure(Exception("Booking not found"))
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelBooking(bookingId: String): Result<Booking> {
        return try {
            delay(300)
            val bookingRef = db.collection(FirestorePaths.BOOKINGS).document(bookingId)
            val existingDoc = bookingRef.get().await()
            val existing = parseBooking(existingDoc.data)
                ?: return Result.failure(Exception("Booking not found"))
            val routeId = existing.route.id
            val cancelled = existing.copy(status = BookingStatus.CANCELLED)
            val cancelledJson = gson.toJson(cancelled)
            db.runTransaction { tx ->
                for (s in existing.seats) {
                    val seatRef = db.collection(FirestorePaths.ROUTES).document(routeId)
                        .collection(FirestorePaths.SEATS).document(s.number)
                    val snap = tx.get(seatRef)
                    if (snap.exists()) {
                        val bid = snap.getString("bookingId")
                        if (bid == bookingId) {
                            tx.update(
                                seatRef,
                                mapOf(
                                    "status" to "AVAILABLE",
                                    "bookingId" to FieldValue.delete()
                                )
                            )
                        }
                    }
                }
                tx.update(
                    bookingRef,
                    mapOf(
                        "status" to BookingStatus.CANCELLED.name,
                        "bookingJson" to cancelledJson
                    )
                )
                null
            }.await()
            Result.success(cancelled)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentRouteIds(): List<String> {
        return allBookingsForUser().getOrNull()
            .orEmpty()
            .sortedByDescending { it.bookingDate }
            .take(3)
            .map { it.route.id }
    }
}
