package com.example.nexiride2.domain.repository

import com.example.nexiride2.domain.model.Driver
import kotlinx.coroutines.flow.Flow

/**
 * Driver-side authentication + profile. Logging in / signing up uses the same
 * Firebase Auth backend as regular users; the presence of a `drivers/{uid}`
 * document is what tells the app to route the driver to the driver portal
 * instead of the passenger home.
 */
interface DriverRepository {
    /** Creates a Firebase Auth user + `drivers/{uid}` doc and signs the driver in. */
    suspend fun signUpDriver(
        fullName: String,
        email: String,
        phone: String,
        licenseNumber: String,
        companyName: String,
        busType: String,
        busCapacity: Int,
        serviceStations: List<String>,
        password: String
    ): Result<Driver>

    /** Signs in via Firebase Auth, then verifies there's a matching driver doc. */
    suspend fun loginDriver(email: String, password: String): Result<Driver>

    /** Reads the driver profile for the currently signed-in uid (null if none). */
    suspend fun getCurrentDriver(): Result<Driver?>

    /** Live stream: current driver profile, or null when signed out / not a driver. */
    fun observeCurrentDriver(): Flow<Driver?>
}
