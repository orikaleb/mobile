package com.example.nexiride2.data.firebase

import com.example.nexiride2.domain.model.Driver
import com.example.nexiride2.domain.repository.DriverRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDriverRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : DriverRepository {

    private fun driverDoc(uid: String) = db.collection(FirestorePaths.DRIVERS).document(uid)

    private fun DocumentSnapshot.toDriver(): Driver? {
        if (!exists()) return null
        val email = getString("email").orEmpty()
        val name = getString("fullName").orEmpty()
        @Suppress("UNCHECKED_CAST")
        val stations = (get("serviceStations") as? List<String>).orEmpty()
        val capacity = (getLong("busCapacity") ?: 0L).toInt()
        return Driver(
            id = id,
            fullName = name.ifBlank { email.substringBefore('@') },
            email = email,
            phone = getString("phone").orEmpty(),
            licenseNumber = getString("licenseNumber").orEmpty(),
            companyName = getString("companyName").orEmpty(),
            assignedBusId = getString("assignedBusId"),
            assignedBusNumber = getString("assignedBusNumber"),
            busType = getString("busType").orEmpty(),
            busCapacity = capacity,
            serviceStations = stations,
            active = getBoolean("active") ?: true,
            avatarUrl = getString("avatarUrl")
        )
    }

    override suspend fun signUpDriver(
        fullName: String,
        email: String,
        phone: String,
        licenseNumber: String,
        companyName: String,
        busType: String,
        busCapacity: Int,
        serviceStations: List<String>,
        password: String
    ): Result<Driver> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user!!
        try {
            runCatching {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(fullName).build()).await()
            }
            // Ensure Firestore sees request.auth on the very next write.
            runCatching { user.getIdToken(true).await() }
            val doc = mapOf(
                "fullName" to fullName,
                "email" to (user.email ?: email.trim()),
                "phone" to phone,
                "licenseNumber" to licenseNumber,
                "companyName" to companyName,
                "busType" to busType,
                "busCapacity" to busCapacity,
                "serviceStations" to serviceStations,
                "active" to true,
                "createdAt" to Timestamp.now()
            )
            driverDoc(user.uid).set(doc, SetOptions.merge()).await()
            Driver(
                id = user.uid,
                fullName = fullName,
                email = user.email ?: email.trim(),
                phone = phone,
                licenseNumber = licenseNumber,
                companyName = companyName,
                busType = busType,
                busCapacity = busCapacity,
                serviceStations = serviceStations,
                active = true
            )
        } catch (t: Throwable) {
            // If writing the driver profile fails (e.g. Firestore rules not yet
            // deployed -> PERMISSION_DENIED) roll back the Firebase Auth user
            // we just created. Otherwise the email is burned and retrying will
            // fail with "email already in use".
            runCatching { user.delete().await() }
            runCatching { auth.signOut() }
            val msg = t.message.orEmpty()
            if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                throw IllegalStateException(
                    "Couldn't save your driver profile because the Firestore " +
                        "rules haven't been deployed. Ask the admin to deploy " +
                        "the latest rules, then try again."
                )
            }
            throw t
        }
    }

    override suspend fun loginDriver(email: String, password: String): Result<Driver> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        val uid = auth.currentUser?.uid ?: error("Sign-in returned no user")
        val driver = driverDoc(uid).get().await().toDriver()
        if (driver == null) {
            // They authenticated, but they're not a registered driver. Sign them
            // back out so we don't leave them in a half-state and so the regular
            // user screens don't pick them up by mistake.
            auth.signOut()
            error("This account isn't registered as a driver.")
        }
        if (!driver.active) {
            auth.signOut()
            error("Your driver account is disabled. Contact the admin.")
        }
        driver
    }

    override suspend fun getCurrentDriver(): Result<Driver?> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching null
        driverDoc(uid).get().await().toDriver()
    }

    override fun observeCurrentDriver(): Flow<Driver?> = callbackFlow {
        // Emit on every Firebase auth-state change — we re-fetch the doc each
        // time because assignedBusId / active may be mutated server-side.
        suspend fun emit() {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                trySend(null)
                return
            }
            val driver = runCatching { driverDoc(uid).get().await().toDriver() }.getOrNull()
            trySend(driver)
        }
        val listener = FirebaseAuth.AuthStateListener { launch { emit() } }
        launch { emit() }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged { old, new ->
        old?.id == new?.id && old?.assignedBusId == new?.assignedBusId && old?.active == new?.active
    }
}
