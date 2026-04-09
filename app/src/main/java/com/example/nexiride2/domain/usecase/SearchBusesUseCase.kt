package com.example.nexiride2.domain.usecase

import com.example.nexiride2.domain.model.RouteSearchResult
import com.example.nexiride2.domain.repository.BusRepository

class SearchBusesUseCase(private val busRepository: BusRepository) {
    suspend operator fun invoke(origin: String, destination: String, date: String, passengers: Int): Result<RouteSearchResult> {
        if (origin.isBlank() || destination.isBlank()) return Result.failure(IllegalArgumentException("Origin and destination are required"))
        if (origin == destination) return Result.failure(IllegalArgumentException("Origin and destination must differ"))
        return busRepository.searchRoutes(origin, destination, date, passengers)
    }
}
