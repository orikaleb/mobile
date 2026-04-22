package com.example.nexiride2.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.repository.BusRepository
import com.example.nexiride2.domain.usecase.SearchBusesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val isLoading: Boolean = false,
    val results: List<Route> = emptyList(),
    val filteredResults: List<Route> = emptyList(),
    val cacheHint: String? = null,
    val selectedRoute: Route? = null,
    val seats: List<Seat> = emptyList(),
    val error: String? = null,
    val origin: String = "",
    val destination: String = "",
    val date: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
    val passengers: Int = 1,
    val cities: List<String> = emptyList(),
    /** Fires once after a successful search so the host can open results. */
    val navigateToResults: Boolean = false,
    /** One-shot message surfaced after resolving the user's GPS fix. */
    val locationMessage: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val busRepository: BusRepository,
    private val searchUseCase: SearchBusesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init { viewModelScope.launch { _uiState.value = _uiState.value.copy(cities = busRepository.getAvailableCities()) } }

    fun updateOrigin(origin: String) { _uiState.value = _uiState.value.copy(origin = origin) }
    fun updateDestination(dest: String) { _uiState.value = _uiState.value.copy(destination = dest) }
    fun updateDate(date: String) { _uiState.value = _uiState.value.copy(date = date) }
    fun updatePassengers(p: Int) { _uiState.value = _uiState.value.copy(passengers = p) }

    fun search() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, cacheHint = null, navigateToResults = false)
        searchUseCase(_uiState.value.origin, _uiState.value.destination, _uiState.value.date, _uiState.value.passengers).fold(
            onSuccess = { payload ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = payload.routes,
                    filteredResults = payload.routes,
                    cacheHint = if (payload.fromCache) {
                        "Showing saved results (device offline or network unavailable)."
                    } else {
                        null
                    },
                    navigateToResults = true
                )
            },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun consumeNavigateToResults() {
        _uiState.value = _uiState.value.copy(navigateToResults = false)
    }

    /**
     * Resolves a GPS fix into one of the supported cities. Matching order:
     *
     *  1. [detectedLocality] (a Geocoder result from the screen) compared to
     *     the city list, exact first, then substring (e.g. "Greater Accra Region"
     *     → "Accra").
     *  2. Nearest supported city, but only if the great-circle distance is within
     *     [CityCoordinates.MAX_MATCH_DISTANCE_KM]. Beyond that we refuse to guess
     *     because it usually means the user is outside Ghana or on an emulator
     *     with a default overseas location.
     *
     * [detectedAddress] is just for the user-facing message.
     */
    fun useCurrentLocation(
        lat: Double,
        lng: Double,
        detectedLocality: String? = null,
        detectedAddress: String? = null
    ) {
        // Reject fixes obviously outside Ghana — the emulator's default fix is in
        // California and would otherwise map to some random city hundreds of km away.
        if (!CityCoordinates.isInGhana(lat, lng)) {
            _uiState.value = _uiState.value.copy(
                locationMessage = "Your location (${fmt(lat)}, ${fmt(lng)}) is outside Ghana — " +
                    "pick your origin city manually."
            )
            return
        }

        val exact = CityCoordinates.matchExact(detectedLocality)
        val contains = exact ?: CityCoordinates.matchContains(detectedLocality)

        if (contains != null) {
            _uiState.value = _uiState.value.copy(
                origin = contains,
                locationMessage = buildString {
                    append("Detected ")
                    append(detectedAddress ?: contains)
                    append(" • using $contains as origin")
                }
            )
            return
        }

        val nearest = CityCoordinates.nearestWithDistance(lat, lng)
        if (nearest == null) {
            _uiState.value = _uiState.value.copy(
                locationMessage = "Couldn't resolve your location."
            )
            return
        }
        if (nearest.distanceKm > CityCoordinates.MAX_MATCH_DISTANCE_KM) {
            _uiState.value = _uiState.value.copy(
                locationMessage = "No supported station within ${CityCoordinates.MAX_MATCH_DISTANCE_KM.toInt()} km " +
                    "(${"%.0f".format(nearest.distanceKm)} km to ${nearest.city}). Pick manually."
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            origin = nearest.city,
            locationMessage = "Using your location • nearest station: ${nearest.city} " +
                "(${"%.0f".format(nearest.distanceKm)} km away)"
        )
    }

    private fun fmt(v: Double): String = "%.3f".format(v)

    fun clearLocationMessage() {
        _uiState.value = _uiState.value.copy(locationMessage = null)
    }

    fun searchWithParams(origin: String, destination: String) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        _uiState.value = _uiState.value.copy(origin = origin, destination = destination, date = today)
        search()
    }

    fun applyFilters(priceRange: ClosedFloatingPointRange<Float>, timeFilter: String, company: String) {
        val filtered = _uiState.value.results.filter { route ->
            route.price >= priceRange.start && route.price <= priceRange.endInclusive &&
            (company == "All" || route.bus.companyName == company) &&
            when (timeFilter) {
                "Morning" -> route.departureTime.substringBefore(":").toIntOrNull()?.let { it in 5..11 } ?: true
                "Afternoon" -> route.departureTime.substringBefore(":").toIntOrNull()?.let { it in 12..16 } ?: true
                "Evening" -> route.departureTime.substringBefore(":").toIntOrNull()?.let { it in 17..23 } ?: true
                else -> true
            }
        }
        _uiState.value = _uiState.value.copy(filteredResults = filtered)
    }

    fun selectRoute(routeId: String) = viewModelScope.launch {
        val travelDate = _uiState.value.date
        busRepository.getRouteById(routeId).onSuccess { route ->
            _uiState.value = _uiState.value.copy(selectedRoute = route.copy(date = travelDate))
        }
    }

    fun loadSeats(routeId: String) = viewModelScope.launch {
        busRepository.getSeatsForRoute(routeId).onSuccess { seats ->
            _uiState.value = _uiState.value.copy(seats = seats)
        }
    }

    /** Reload seat map from Firestore (e.g. after another booking). */
    fun refreshSeats(routeId: String) = loadSeats(routeId)

    fun getCompanies(): List<String> = _uiState.value.results.map { it.bus.companyName }.distinct()
}
