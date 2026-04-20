package com.example.nexiride2.data.repository

import com.example.nexiride2.data.connectivity.NetworkStatusProvider
import com.example.nexiride2.data.supabase.SupabaseBusRepository
import com.example.nexiride2.data.local.db.RouteCacheDao
import com.example.nexiride2.data.local.db.RouteCacheEntity
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.RouteSearchResult
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.repository.BusRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class CachingBusRepository @Inject constructor(
    private val remote: SupabaseBusRepository,
    private val routeCacheDao: RouteCacheDao,
    private val gson: Gson,
    private val networkStatus: NetworkStatusProvider
) : BusRepository {

    private val listRouteType = object : TypeToken<ArrayList<Route>>() {}.type

    override suspend fun searchRoutes(
        origin: String,
        destination: String,
        date: String,
        passengers: Int
    ): Result<RouteSearchResult> {
        val key = cacheKeyForSearch(origin, destination, date, passengers)
        if (!networkStatus.isConnected()) {
            val cached = loadRoutesSuspend(key)
            return if (cached != null) {
                Result.success(RouteSearchResult(cached, fromCache = true))
            } else {
                Result.failure(
                    Exception("You're offline and there is no saved search for this trip yet. Connect once, search, then try again offline.")
                )
            }
        }
        return remote.searchRoutes(origin, destination, date, passengers).fold(
            onSuccess = { payload ->
                saveRoutes(key, payload.routes)
                Result.success(RouteSearchResult(payload.routes, fromCache = false))
            },
            onFailure = { err ->
                val cached = loadRoutesSuspend(key)
                if (cached != null) Result.success(RouteSearchResult(cached, fromCache = true))
                else Result.failure(err)
            }
        )
    }

    override suspend fun getRouteById(routeId: String): Result<Route> =
        remote.getRouteById(routeId)

    override suspend fun getSeatsForRoute(routeId: String): Result<List<Seat>> =
        remote.getSeatsForRoute(routeId)

    override suspend fun getPopularRoutes(): Result<List<Route>> {
        val key = KEY_POPULAR
        if (!networkStatus.isConnected()) {
            val cached = loadRoutesSuspend(key)
            return if (cached != null) Result.success(cached)
            else Result.failure(Exception("Offline with no cached popular routes."))
        }
        return remote.getPopularRoutes().fold(
            onSuccess = { routes ->
                saveRoutes(key, routes)
                Result.success(routes)
            },
            onFailure = { e ->
                val cached = loadRoutesSuspend(key)
                if (cached != null) Result.success(cached) else Result.failure(e)
            }
        )
    }

    override suspend fun getRoutesByDestination(destination: String): Result<List<Route>> {
        val key = cacheKeyForDestination(destination)
        if (!networkStatus.isConnected()) {
            val cached = loadRoutesSuspend(key)
            return if (cached != null) Result.success(cached)
            else Result.failure(Exception("Offline with no saved routes for this destination."))
        }
        return remote.getRoutesByDestination(destination).fold(
            onSuccess = { routes ->
                saveRoutes(key, routes)
                Result.success(routes)
            },
            onFailure = { e ->
                val cached = loadRoutesSuspend(key)
                if (cached != null) Result.success(cached) else Result.failure(e)
            }
        )
    }

    override suspend fun getAvailableCities(): List<String> = remote.getAvailableCities()

    private suspend fun loadRoutesSuspend(key: String): List<Route>? {
        val row = routeCacheDao.getByKey(key) ?: return null
        return runCatching { gson.fromJson<ArrayList<Route>>(row.routesJson, listRouteType) }
            .getOrNull()
            ?.toList()
    }

    private suspend fun saveRoutes(key: String, routes: List<Route>) {
        routeCacheDao.upsert(
            RouteCacheEntity(
                cacheKey = key,
                routesJson = gson.toJson(routes, listRouteType),
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        const val KEY_POPULAR = "cache:popular_routes"

        fun cacheKeyForSearch(origin: String, destination: String, date: String, passengers: Int): String =
            "search:${origin.trim().lowercase()}|${destination.trim().lowercase()}|$date|$passengers"

        fun cacheKeyForDestination(destination: String): String =
            "cache:destination:${destination.trim().lowercase()}"
    }
}
