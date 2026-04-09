package com.example.nexiride2.domain.repository

import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.RouteSearchResult
import com.example.nexiride2.domain.model.Seat

interface BusRepository {
    suspend fun searchRoutes(origin: String, destination: String, date: String, passengers: Int): Result<RouteSearchResult>
    suspend fun getRouteById(routeId: String): Result<Route>
    suspend fun getSeatsForRoute(routeId: String): Result<List<Seat>>
    suspend fun getPopularRoutes(): Result<List<Route>>
    suspend fun getAvailableCities(): List<String>
}
