package com.example.nexiride2.data.firebase

import com.example.nexiride2.domain.model.Booking
import com.example.nexiride2.domain.model.BookingStatus
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AdminBookingEntry
import com.example.nexiride2.domain.repository.AdminRepository
import com.example.nexiride2.domain.repository.BookingRepository
import com.example.nexiride2.domain.repository.BroadcastResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreAdminRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val gson: Gson,
    private val bookingRepository: BookingRepository
) : AdminRepository {

    override suspend fun listAllUsers(): Result<List<User>> = runCatching {
        val snap = db.collection(FirestorePaths.USERS).get().await()
        snap.documents
            .map { d ->
                val email = d.getString("email").orEmpty()
                val rawName = d.getString("fullName").orEmpty()
                // Prefer the full name; if missing, fall back to the email local-part
                // so every row in the admin list is still identifiable.
                val resolvedName = rawName.ifBlank {
                    email.substringBefore('@').ifBlank { "—" }
                }
                User(
                    id = d.id,
                    name = resolvedName,
                    email = email,
                    phone = d.getString("phone").orEmpty(),
                    profilePhotoUrl = d.getString("avatarUrl")
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    override suspend fun listAllBookings(): Result<List<AdminBookingEntry>> = runCatching {
        val snap = db.collection(FirestorePaths.BOOKINGS).get().await()
        snap.documents.mapNotNull { d ->
            val json = d.getString("bookingJson") ?: return@mapNotNull null
            val booking = runCatching { gson.fromJson(json, Booking::class.java) }.getOrNull()
                ?: return@mapNotNull null
            AdminBookingEntry(
                booking = booking,
                userId = d.getString("userId").orEmpty()
            )
        }.sortedByDescending { it.booking.bookingDate }
    }

    override suspend fun cancelBookingAsAdmin(bookingId: String): Result<Booking> {
        // Reuses the existing cancel-booking transaction (seat refund + status flip).
        // The Firestore rule update above lets admins call it on anyone's booking.
        return bookingRepository.cancelBooking(bookingId).map { cancelled ->
            if (cancelled.status != BookingStatus.CANCELLED) cancelled.copy(status = BookingStatus.CANCELLED)
            else cancelled
        }
    }

    override suspend fun broadcastNotification(
        title: String,
        message: String
    ): Result<BroadcastResult> = runCatching {
        val users = db.collection(FirestorePaths.USERS).get().await()
        val now = Timestamp.now()
        // Firestore batched writes cap at 500 ops — chunk to stay safe with large user lists.
        var count = 0
        users.documents.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { userDoc ->
                val notifId = UUID.randomUUID().toString()
                val ref = db.collection(FirestorePaths.NOTIFICATIONS).document(notifId)
                batch.set(
                    ref,
                    mapOf(
                        "id" to notifId,
                        "userId" to userDoc.id,
                        "title" to title,
                        "message" to message,
                        "type" to "GENERAL",
                        "isRead" to false,
                        "createdAt" to now
                    )
                )
                count++
            }
            batch.commit().await()
        }
        BroadcastResult(recipients = count)
    }
}
