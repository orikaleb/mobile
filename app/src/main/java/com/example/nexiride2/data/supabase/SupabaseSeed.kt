package com.example.nexiride2.data.supabase

import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.local.MockData
import com.example.nexiride2.data.supabase.dto.CityRowDto
import com.example.nexiride2.data.supabase.dto.RouteRowDto
import com.example.nexiride2.data.supabase.dto.RouteSeatInsertDto
import com.example.nexiride2.domain.model.Route
import com.google.gson.Gson
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Debug-only seed: uploads bundled [MockData] routes/cities and seat rows when tables are empty.
 * Bookings require a real [auth.users] id — create an account in the app instead of seeding bookings here.
 */
object SupabaseSeed {

    private val started = AtomicBoolean(false)

    suspend fun seedIfEmpty(api: SupabasePostgrestApi, gson: Gson) {
        if (!BuildConfig.DEBUG) return
        if (BuildConfig.SUPABASE_ANON_KEY.isBlank()) return
        if (!started.compareAndSet(false, true)) return

        runCatching {
            val existing = api.listAllRoutes()
            if (existing.isEmpty()) {
                val rows = MockData.routes.map { route ->
                    RouteRowDto(
                        id = route.id,
                        origin = route.origin,
                        destination = route.destination,
                        date = route.date,
                        availableSeats = route.availableSeats,
                        routeJson = gson.toJson(route)
                    )
                }
                api.insertRoutes(rows)
            }
        }

        runCatching {
            val cities = api.listCities()
            if (cities.isEmpty()) {
                val rows = MockData.cities.map { CityRowDto(it) }
                api.insertCities(rows)
            }
        }

        runCatching {
            val routes = api.listAllRoutes()
            if (routes.isEmpty()) return@runCatching
            val probe = api.routeSeatsByRoute("eq.${routes.first().id}")
            if (probe.isNotEmpty()) return@runCatching
            for (route in routes) {
                val domain: Route? = runCatching { gson.fromJson(route.routeJson, Route::class.java) }.getOrNull()
                val total = domain?.bus?.totalSeats ?: 45
                val inserts = (1..total).map { n ->
                    RouteSeatInsertDto(
                        routeId = route.id,
                        seatNumber = n.toString(),
                        rowIdx = (n - 1) / 4 + 1,
                        colIdx = (n - 1) % 4 + 1,
                        status = "AVAILABLE"
                    )
                }
                api.insertRouteSeats(inserts)
            }
        }
    }
}
