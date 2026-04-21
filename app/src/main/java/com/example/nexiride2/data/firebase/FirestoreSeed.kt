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
                val batch = db.batch()
                MockData.routes.forEach { route ->
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

        runCatching {
            for (route in MockData.routes) {
                val seatsCol = db.collection(FirestorePaths.ROUTES).document(route.id)
                    .collection(FirestorePaths.SEATS)
                val first = seatsCol.limit(1).get().await()
                if (first.isEmpty) {
                    val batch = db.batch()
                    val total = route.bus.totalSeats
                    for (n in 1..total) {
                        val doc = seatsCol.document(n.toString())
                        batch.set(
                            doc,
                            mapOf(
                                "status" to "AVAILABLE",
                                "rowIdx" to (n - 1) / 4 + 1,
                                "colIdx" to (n - 1) % 4 + 1
                            )
                        )
                    }
                    batch.commit().await()
                }
            }
        }
    }
}
