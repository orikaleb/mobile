package com.example.nexiride2.domain.repository

import com.example.nexiride2.domain.model.Booking
import com.example.nexiride2.domain.model.Driver
import com.example.nexiride2.domain.model.FleetBus
import com.example.nexiride2.domain.model.User

/** A booking plus the user id it belongs to (needed when listing across all users). */
data class AdminBookingEntry(
    val booking: Booking,
    val userId: String
)

/** The result of an admin broadcast: how many notification docs were written. */
data class BroadcastResult(val recipients: Int)

/**
 * Operations only reachable from the Admin console. All calls require the caller
 * to be on the Firestore allow-list (see `firestore.rules` and `AdminConfig`).
 */
interface AdminRepository {
    suspend fun listAllUsers(): Result<List<User>>
    suspend fun listAllBookings(): Result<List<AdminBookingEntry>>
    suspend fun cancelBookingAsAdmin(bookingId: String): Result<Booking>
    suspend fun broadcastNotification(title: String, message: String): Result<BroadcastResult>

    /** All registered bus drivers; used by the admin Fleet tab. */
    suspend fun listAllDrivers(): Result<List<Driver>>
    /** All registered fleet buses. */
    suspend fun listAllBuses(): Result<List<FleetBus>>
    /** Creates or updates a fleet bus (id is generated if null). */
    suspend fun upsertBus(bus: FleetBus): Result<FleetBus>
    /** Removes a fleet bus from the registry. */
    suspend fun deleteBus(busId: String): Result<Unit>
    /** Flips the `active` flag on a driver account. */
    suspend fun setDriverActive(driverId: String, active: Boolean): Result<Unit>
    /** Assigns a fleet bus registration number to a driver (or clears it). */
    suspend fun assignDriverBus(driverId: String, bus: FleetBus?): Result<Unit>
}
