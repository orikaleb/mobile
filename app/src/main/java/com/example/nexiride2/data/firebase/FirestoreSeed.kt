package com.example.nexiride2.data.firebase

import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.local.MockData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Debug-only seed: writes bundled [MockData] routes, cities, and per-route seat docs when collections are empty.
 */
object FirestoreSeed {

    private val started = AtomicBoolean(false)

    suspend fun seedIfEmpty(db: FirebaseFirestore, gson: Gson) {
        if (!BuildConfig.DEBUG) return
        if (!started.compareAndSet(false, true)) return

        runCatching {
            val probe = db.collection(FirestorePaths.ROUTES).limit(1).get().await()
            if (probe.isEmpty) {
                // Firestore batch max 500 ops — one set per route (all city pairs).
                MockData.routes.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { route ->
                        val ref = db.collection(FirestorePaths.ROUTES).document(route.id)
                        batch.set(
                            ref,
                            mapOf(
                                "id" to route.id,
                                "origin" to route.origin,
                                "destination" to route.destination,
                                "date" to route.date,
                                "availableSeats" to route.availableSeats,
                                "routeJson" to gson.toJson(route)
                            )
                        )
                    }
                    batch.commit().await()
                }
            }
        }

        runCatching {
            val citiesProbe = db.collection(FirestorePaths.CITIES).limit(1).get().await()
            if (citiesProbe.isEmpty) {
                val batch = db.batch()
                MockData.cities.forEach { name ->
                    val ref = db.collection(FirestorePaths.CITIES).document()
                    batch.set(ref, mapOf("name" to name))
                }
                batch.commit().await()
            }
        }

        // Seat subdocs are created on first booking (see FirestoreBookingRepository) or read as mock when empty.
    }
}
