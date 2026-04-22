package com.example.nexiride2.domain.usecase

import com.example.nexiride2.domain.model.RouteSearchResult
import com.example.nexiride2.domain.repository.BusRepository
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SearchBusesUseCase(private val busRepository: BusRepository) {
    suspend operator fun invoke(origin: String, destination: String, date: String, passengers: Int): Result<RouteSearchResult> {
        if (origin.isBlank() || destination.isBlank()) return Result.failure(IllegalArgumentException("Origin and destination are required"))
        if (origin == destination) return Result.failure(IllegalArgumentException("Origin and destination must differ"))
        if (isDateInPastYmd(date)) {
            return Result.failure(IllegalArgumentException("Choose today or a future travel date."))
        }
        return busRepository.searchRoutes(origin, destination, date, passengers)
    }

    private fun isDateInPastYmd(ymd: String): Boolean {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false; timeZone = TimeZone.getDefault() }
            val tripStart = fmt.parse(ymd) ?: return true
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            tripStart.before(startOfToday)
        } catch (_: ParseException) {
            true
        }
    }
}
