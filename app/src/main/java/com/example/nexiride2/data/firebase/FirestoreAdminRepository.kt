package com.example.nexiride2.data.firebase

import com.example.nexiride2.domain.model.Booking
import com.example.nexiride2.domain.model.BookingStatus
import com.example.nexiride2.domain.model.Driver
import com.example.nexiride2.domain.model.FleetBus
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AdminBookingEntry
import com.example.nexiride2.domain.repository.AdminRepository
import com.example.nexiride2.domain.repository.BookingRepository
import com.example.nexiride2.domain.repository.BroadcastResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    override suspend fun listAllDrivers(): Result<List<Driver>> = runCatching {
        val snap = db.collection(FirestorePaths.DRIVERS).get().await()
        snap.documents.map { d ->
            val email = d.getString("email").orEmpty()
            val rawName = d.getString("fullName").orEmpty()
            Driver(
                id = d.id,
                fullName = rawName.ifBlank { email.substringBefore('@').ifBlank { "—" } },
                email = email,
                phone = d.getString("phone").orEmpty(),
                licenseNumber = d.getString("licenseNumber").orEmpty(),
                companyName = d.getString("companyName").orEmpty(),
                assignedBusId = d.getString("assignedBusId"),
                assignedBusNumber = d.getString("assignedBusNumber"),
                active = d.getBoolean("active") ?: true,
                avatarUrl = d.getString("avatarUrl")
            )
        }.sortedBy { it.fullName.lowercase() }
    }

    override suspend fun listAllBuses(): Result<List<FleetBus>> = runCatching {
        val snap = db.collection(FirestorePaths.BUSES).get().await()
        snap.documents.map { d ->
            @Suppress("UNCHECKED_CAST")
            val amenities = (d.get("amenities") as? List<String>).orEmpty()
            FleetBus(
                id = d.id,
                busNumber = d.getString("busNumber").orEmpty(),
                companyName = d.getString("companyName").orEmpty(),
                busType = d.getString("busType").orEmpty(),
                totalSeats = (d.getLong("totalSeats") ?: 0L).toInt(),
                amenities = amenities,
                driverId = d.getString("driverId"),
                active = d.getBoolean("active") ?: true
            )
        }.sortedBy { it.busNumber.lowercase() }
    }

    override suspend fun upsertBus(bus: FleetBus): Result<FleetBus> = runCatching {
        val id = bus.id.ifBlank { UUID.randomUUID().toString() }
        val ref = db.collection(FirestorePaths.BUSES).document(id)
        val payload = mapOf(
            "busNumber" to bus.busNumber,
            "companyName" to bus.companyName,
            "busType" to bus.busType,
            "totalSeats" to bus.totalSeats,
            "amenities" to bus.amenities,
            "driverId" to bus.driverId,
            "active" to bus.active,
            "updatedAt" to Timestamp.now()
        )
        ref.set(payload, SetOptions.merge()).await()
        bus.copy(id = id)
    }

    override suspend fun deleteBus(busId: String): Result<Unit> = runCatching {
        db.collection(FirestorePaths.BUSES).document(busId).delete().await()
    }

    override suspend fun setDriverActive(driverId: String, active: Boolean): Result<Unit> = runCatching {
        db.collection(FirestorePaths.DRIVERS).document(driverId)
            .set(mapOf("active" to active, "updatedAt" to Timestamp.now()), SetOptions.merge())
            .await()
    }

    override suspend fun assignDriverBus(driverId: String, bus: FleetBus?): Result<Unit> = runCatching {
        val payload = mapOf(
            "assignedBusId" to bus?.id,
            "assignedBusNumber" to bus?.busNumber,
            "updatedAt" to Timestamp.now()
        )
        db.collection(FirestorePaths.DRIVERS).document(driverId)
            .set(payload, SetOptions.merge())
            .await()
        // Mirror the driver linkage on the bus doc so /buses is a usable fleet view.
        if (bus != null) {
            db.collection(FirestorePaths.BUSES).document(bus.id)
                .set(mapOf("driverId" to driverId, "updatedAt" to Timestamp.now()), SetOptions.merge())
                .await()
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
