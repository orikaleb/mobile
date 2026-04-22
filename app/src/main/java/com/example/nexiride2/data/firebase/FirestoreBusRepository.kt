package com.example.nexiride2.data.firebase

import com.example.nexiride2.data.local.MockData
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.RouteSearchResult
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.model.SeatStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBusRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val gson: Gson
) {

    private fun parseRoute(data: Map<String, Any>?): Route? {
        val json = data?.get("routeJson") as? String ?: return null
        return runCatching { gson.fromJson(json, Route::class.java) }.getOrNull()
    }

    /** Firestore whereEqualTo is case-sensitive; align with city names in documents. */
    private fun titleCaseCity(raw: String): String =
        raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    suspend fun searchRoutes(
        origin: String,
        destination: String,
        date: String,
        passengers: Int
    ): Result<RouteSearchResult> {
        return try {
            delay(120)
            val originTrim = titleCaseCity(origin)
            val destTrim = titleCaseCity(destination)
            val remote = runCatching {
                val snap = db.collection(FirestorePaths.ROUTES)
                    .whereEqualTo("origin", originTrim)
                    .get()
                    .await()
                snap.documents.mapNotNull { parseRoute(it.data) }
                    .filter {
                        it.origin.equals(originTrim, ignoreCase = true) &&
                            it.destination.equals(destTrim, ignoreCase = true) &&
                            it.availableSeats >= passengers
                    }
            }.getOrDefault(emptyList())
            // Also use the bundled inventory so every city pair + any day works.
            val local = MockData.routes.filter {
                it.origin.equals(originTrim, ignoreCase = true) &&
                    it.destination.equals(destTrim, ignoreCase = true) &&
                    it.availableSeats >= passengers
            }
            val candidates = (remote + local).distinctBy { it.id }
            if (candidates.isEmpty()) return Result.success(RouteSearchResult(emptyList(), fromCache = false))
            val routes = candidates.map { it.copy(date = date) }
                .sortedBy { it.departureTime }
            Result.success(RouteSearchResult(routes, fromCache = false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRouteById(routeId: String): Result<Route> {
        return try {
            delay(80)
            val doc = runCatching {
                db.collection(FirestorePaths.ROUTES).document(routeId).get().await()
            }.getOrNull()
            val remote = doc?.let { parseRoute(it.data) }
            val route = remote ?: MockData.routes.find { it.id == routeId }
                ?: return Result.failure(Exception("Route not found"))
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeatsForRoute(routeId: String): Result<List<Seat>> {
        return try {
            delay(80)
            val route = getRouteById(routeId).getOrElse { return Result.failure(it) }
            val seatsSnap = db.collection(FirestorePaths.ROUTES).document(routeId)
                .collection(FirestorePaths.SEATS)
                .get()
                .await()
            if (seatsSnap.isEmpty) {
                return Result.success(MockData.generateSeats(routeId))
            }
            val total = route.bus.totalSeats
            val seats = seatsSnap.documents
                .sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
                .take(total)
                .map { d ->
                    val status = (d.getString("status") ?: "AVAILABLE").uppercase()
                    Seat(
                        id = "${routeId}_${d.id}",
                        number = d.id,
                        row = (d.getLong("rowIdx") ?: 1L).toInt(),
                        column = (d.getLong("colIdx") ?: 1L).toInt(),
                        status = if (status == "BOOKED") SeatStatus.RESERVED else SeatStatus.AVAILABLE,
                        price = route.price
                    )
                }
            Result.success(seats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularRoutes(): Result<List<Route>> {
        return try {
            delay(100)
            val remote = runCatching {
                db.collection(FirestorePaths.ROUTES).limit(40).get().await()
                    .documents.mapNotNull { parseRoute(it.data) }
            }.getOrDefault(emptyList())
            val pool = (remote + MockData.routes).distinctBy { "${it.origin}->${it.destination}" }
            Result.success(pool.take(6))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoutesByDestination(destination: String): Result<List<Route>> {
        val dest = titleCaseCity(destination)
        if (dest.isEmpty()) return Result.success(emptyList())
        return try {
            delay(80)
            val remote = runCatching {
                db.collection(FirestorePaths.ROUTES)
                    .whereEqualTo("destination", dest)
                    .get()
                    .await()
                    .documents.mapNotNull { parseRoute(it.data) }
            }.getOrDefault(emptyList())
            val local = MockData.routes.filter { it.destination.equals(dest, ignoreCase = true) }
            Result.success((remote + local).distinctBy { it.id })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableCities(): List<String> {
        val remote = runCatching {
            db.collection(FirestorePaths.CITIES).get().await()
                .documents.mapNotNull { it.getString("name") }
        }.getOrDefault(emptyList())
        return (remote + MockData.cities).distinct().sorted()
    }
}
