package com.example.nexiride2.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.BookingRepository
import com.example.nexiride2.domain.repository.BusRepository
import com.example.nexiride2.domain.repository.UserRepository
import com.example.nexiride2.ui.components.PromoBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val popularRoutes: List<Route> = emptyList(),
    val routesToFeaturedDestination: List<Route> = emptyList(),
    val featuredDestination: String? = null,
    val recentRoutes: List<Route> = emptyList(),
    val cities: List<String> = emptyList(),
    val promoBanners: List<PromoBanner> = listOf(
        PromoBanner("20% Off VIP Routes", "This weekend only!", "NEXIVIP20"),
        PromoBanner("New Route: Accra → Wa", "Book now from GHS 180", "NEWROUTE"),
        PromoBanner("Refer & Earn", "Get GHS 10 for every referral", "REFER10")
    )
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val busRepository: BusRepository,
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init { loadHomeData() }

    private fun loadHomeData() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val user = userRepository.getUser().getOrNull()
        val cities = busRepository.getAvailableCities()
        val popular = busRepository.getPopularRoutes().getOrDefault(emptyList())
        val featuredDest =
            popular.firstOrNull()?.destination?.takeIf { it.isNotBlank() }
                ?: cities.firstOrNull()
        val routesToFeatured =
            featuredDest?.let { busRepository.getRoutesByDestination(it).getOrDefault(emptyList()) }
                ?: emptyList()
        val recentIds = bookingRepository.getRecentRouteIds()
        val recent = recentIds.mapNotNull { id -> busRepository.getRouteById(id).getOrNull() }
        _uiState.value = HomeUiState(
            isLoading = false,
            user = user,
            popularRoutes = popular,
            routesToFeaturedDestination = routesToFeatured,
            featuredDestination = featuredDest,
            recentRoutes = recent,
            cities = cities
        )
    }

    fun refresh() = loadHomeData()
}
