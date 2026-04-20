package com.example.nexiride2.data.repository

import com.example.nexiride2.data.local.MockData
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.RouteSearchResult
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.repository.BusRepository
import kotlinx.coroutines.delay

class MockBusRepository : BusRepository {
    override suspend fun searchRoutes(origin: String, destination: String, date: String, passengers: Int): Result<RouteSearchResult> {
        delay(600)
        val results = MockData.routes.filter {
            it.origin.equals(origin, true) && it.destination.equals(destination, true) && it.availableSeats >= passengers
        }
        return Result.success(RouteSearchResult(results, fromCache = false))
    }

    override suspend fun getRouteById(routeId: String): Result<Route> {
        delay(300)
        return MockData.routes.find { it.id == routeId }?.let { Result.success(it) }
            ?: Result.failure(Exception("Route not found"))
    }

    override suspend fun getSeatsForRoute(routeId: String): Result<List<Seat>> {
        delay(400)
        return Result.success(MockData.generateSeats(routeId))
    }

    override suspend fun getPopularRoutes(): Result<List<Route>> {
        delay(300)
        return Result.success(MockData.routes.take(4))
    }

    override suspend fun getRoutesByDestination(destination: String): Result<List<Route>> {
        delay(250)
        val routes = MockData.routes.filter { it.destination.equals(destination, ignoreCase = true) }
        return Result.success(routes)
    }

    override suspend fun getAvailableCities() = MockData.cities
}
