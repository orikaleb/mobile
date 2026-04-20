package com.example.nexiride2.data.supabase

import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.local.MockData
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.RouteSearchResult
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.model.SeatStatus
import com.google.gson.Gson
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseBusRepository @Inject constructor(
    private val api: SupabasePostgrestApi,
    private val gson: Gson
) {

    private fun configured(): Boolean = BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    suspend fun searchRoutes(
        origin: String,
        destination: String,
        date: String,
        passengers: Int
    ): Result<RouteSearchResult> {
        if (!configured()) {
            return Result.failure(Exception("Add supabase.url and supabase.anon.key to local.properties (see README)."))
        }
        return try {
            delay(200)
            val rows = api.routesByOrigin("eq.$origin")
            val routes = rows.mapNotNull { row ->
                runCatching { gson.fromJson(row.routeJson, Route::class.java) }.getOrNull()
            }.filter {
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
        if (!configured()) {
            return Result.failure(Exception("Supabase is not configured."))
        }
        return try {
            delay(150)
            val row = api.routeById("eq.$routeId").firstOrNull()
                ?: return Result.failure(Exception("Route not found"))
            Result.success(gson.fromJson(row.routeJson, Route::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeatsForRoute(routeId: String): Result<List<Seat>> {
        if (!configured()) {
            delay(200)
            return Result.success(MockData.generateSeats(routeId))
        }
        return try {
            delay(120)
            val route = getRouteById(routeId).getOrElse { return Result.failure(it) }
            val rows = api.routeSeatsByRoute("eq.$routeId")
            if (rows.isEmpty()) {
                return Result.success(MockData.generateSeats(routeId))
            }
            val total = route.bus.totalSeats
            val seats = rows
                .sortedBy { it.seatNumber.toIntOrNull() ?: Int.MAX_VALUE }
                .take(total)
                .map { row ->
                    Seat(
                        id = "${row.routeId}_${row.seatNumber}",
                        number = row.seatNumber,
                        row = row.rowIdx,
                        column = row.colIdx,
                        status = when (row.status.uppercase()) {
                            "BOOKED" -> SeatStatus.RESERVED
                            else -> SeatStatus.AVAILABLE
                        },
                        price = route.price
                    )
                }
            Result.success(seats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularRoutes(): Result<List<Route>> {
        if (!configured()) {
            return Result.failure(Exception("Supabase is not configured."))
        }
        return try {
            delay(200)
            val rows = api.listAllRoutes()
            val routes = rows.mapNotNull {
                runCatching { gson.fromJson(it.routeJson, Route::class.java) }.getOrNull()
            }.sortedBy { it.id }
            Result.success(routes.take(4))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoutesByDestination(destination: String): Result<List<Route>> {
        if (!configured()) {
            return Result.failure(Exception("Supabase is not configured."))
        }
        val dest = destination.trim()
        if (dest.isEmpty()) return Result.success(emptyList())
        return try {
            delay(150)
            val rows = api.routesByDestination("eq.$dest")
            val routes = rows.mapNotNull {
                runCatching { gson.fromJson(it.routeJson, Route::class.java) }.getOrNull()
            }.distinctBy { it.id }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableCities(): List<String> {
        if (!configured()) return MockData.cities
        return try {
            val cities = api.listCities().map { it.name }.distinct().sorted()
            cities.takeIf { it.isNotEmpty() } ?: MockData.cities
        } catch (_: Exception) {
            MockData.cities
        }
    }
}
