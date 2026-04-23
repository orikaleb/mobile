package com.example.nexiride2.domain.model

/**
 * A registered bus driver. The [id] is always the driver's Firebase Auth UID,
 * which keeps Firestore security rules simple (uid-based ownership).
 */
data class Driver(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val licenseNumber: String,
    val companyName: String = "",
    val assignedBusId: String? = null,
    val assignedBusNumber: String? = null,
    /** Body style of the bus the driver operates (e.g. "VIP", "Mini-bus"). */
    val busType: String = "",
    /** Passenger capacity of the driver's bus. 0 if not provided. */
    val busCapacity: Int = 0,
    /** Cities the driver serves — used for matching to routes / filtering. */
    val serviceStations: List<String> = emptyList(),
    val active: Boolean = true,
    val avatarUrl: String? = null,
    val createdAtIso: String? = null
) {
    companion object {
        /** Canonical list of bus types the portal offers at registration. */
        val BUS_TYPES: List<String> = listOf(
            "VIP",
            "Executive Coach",
            "Standard Coach",
            "Mini-bus",
            "Shuttle"
        )
    }
}

/**
 * Minimal fleet bus registry entry (separate from the per-trip [Bus] copy
 * embedded inside a [Route]). This is what the admin fleet page manages.
 */
data class FleetBus(
    val id: String,
    val busNumber: String,
    val companyName: String,
    val busType: String,
    val totalSeats: Int,
    val amenities: List<String> = emptyList(),
    val driverId: String? = null,
    val active: Boolean = true,
    val createdAtIso: String? = null
)
