package com.example.nexiride2.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexiride2.ui.components.PromoCarousel
import com.example.nexiride2.ui.theme.*

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onSearchClick: (String, String) -> Unit,
    onRouteClick: (String) -> Unit,
    onProfileClick: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var fromCity by remember { mutableStateOf("") }
    var toCity by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        // Header with gradient
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(GradientStart, GradientEnd))).statusBarsPadding().padding(20.dp)) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Hello, ${uiState.user?.name?.split(" ")?.firstOrNull() ?: "Traveler"} 👋",
                            style = MaterialTheme.typography.titleMedium, color = SurfaceLight.copy(alpha = 0.8f))
                        Text("Where to next?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = SurfaceLight)
                    }
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(SurfaceLight.copy(alpha = 0.2f))
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = SurfaceLight)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Quick search card
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceLight)) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(value = fromCity, onValueChange = { fromCity = it }, label = { Text("From") },
                            leadingIcon = { Icon(Icons.Default.TripOrigin, null, tint = AccentGreen) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = toCity, onValueChange = { toCity = it }, label = { Text("To") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = StatusError) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onSearchClick(fromCity, toCity) }, modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp), enabled = fromCity.isNotBlank() && toCity.isNotBlank()) {
                            Icon(Icons.Default.Search, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Search Buses", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Quick cities
        Text("Popular Cities", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(uiState.cities.take(6)) { city ->
                SuggestionChip(onClick = { fromCity = city }, label = { Text(city) },
                    icon = { Icon(Icons.Default.LocationCity, null, Modifier.size(16.dp)) })
            }
        }

        Spacer(Modifier.height(24.dp))

        // Promo banners
        Text("Special Offers", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        PromoCarousel(banners = uiState.promoBanners, modifier = Modifier.padding(start = 20.dp))

        Spacer(Modifier.height(24.dp))

        // Recent routes
        if (uiState.recentRoutes.isNotEmpty()) {
            Text("Recent Trips", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.recentRoutes) { route ->
                    Card(Modifier.width(200.dp).clickable { onRouteClick(route.id) }, shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("${route.origin} → ${route.destination}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${route.bus.companyName} • ${route.duration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("GHS ${"%.2f".format(route.price)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Popular routes
        Spacer(Modifier.height(24.dp))
        Text("Popular Routes", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        uiState.popularRoutes.forEach { route ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable { onRouteClick(route.id) },
                shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${route.origin} → ${route.destination}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("${route.departureTime} • ${route.bus.companyName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("GHS ${"%.0f".format(route.price)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}
