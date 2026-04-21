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

    suspend fun searchRoutes(
        origin: String,
        destination: String,
        date: String,
        passengers: Int
    ): Result<RouteSearchResult> {
        return try {
            delay(120)
            val snap = db.collection(FirestorePaths.ROUTES)
                .whereEqualTo("origin", origin.trim())
                .get()
                .await()
            val routes = snap.documents.mapNotNull { parseRoute(it.data) }
                .filter {
                    it.destination.equals(destination, ignoreCase = true) &&
                        it.date == date &&
                        it.availableSeats >= passengers
                }
            Result.success(RouteSearchResult(routes, fromCache = false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRouteById(routeId: String): Result<Route> {
        return try {
            delay(80)
            val doc = db.collection(FirestorePaths.ROUTES).document(routeId).get().await()
            val route = parseRoute(doc.data) ?: return Result.failure(Exception("Route not found"))
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
            val snap = db.collection(FirestorePaths.ROUTES).get().await()
            val routes = snap.documents.mapNotNull { parseRoute(it.data) }.sortedBy { it.id }
            Result.success(routes.take(4))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoutesByDestination(destination: String): Result<List<Route>> {
        val dest = destination.trim()
        if (dest.isEmpty()) return Result.success(emptyList())
        return try {
            delay(80)
            val snap = db.collection(FirestorePaths.ROUTES)
                .whereEqualTo("destination", dest)
                .get()
                .await()
            val routes = snap.documents.mapNotNull { parseRoute(it.data) }.distinctBy { it.id }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableCities(): List<String> {
        return try {
            val snap = db.collection(FirestorePaths.CITIES).get().await()
            val names = snap.documents.mapNotNull { it.getString("name") }.distinct().sorted()
            names.takeIf { it.isNotEmpty() } ?: MockData.cities
        } catch (_: Exception) {
            MockData.cities
        }
    }
}
