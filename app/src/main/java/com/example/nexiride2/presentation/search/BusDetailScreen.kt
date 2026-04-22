package com.example.nexiride2.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexiride2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusDetailScreen(searchViewModel: SearchViewModel, routeId: String, onBack: () -> Unit, onSelectBus: () -> Unit) {
    val uiState by searchViewModel.uiState.collectAsState()
    LaunchedEffect(routeId) { searchViewModel.selectRoute(routeId) }
    val route = uiState.selectedRoute ?: return

    Scaffold(topBar = {
        TopAppBar(title = { Text("Bus Details") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            // Route header
            Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd))).padding(20.dp)) {
                Column {
                    Text(route.bus.companyName, style = MaterialTheme.typography.titleMedium, color = SurfaceLight.copy(alpha = 0.8f))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column { Text(route.departureTime, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = SurfaceLight)
                            Text(route.origin, style = MaterialTheme.typography.bodyMedium, color = SurfaceLight.copy(0.8f)) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(route.duration, style = MaterialTheme.typography.bodySmall, color = SurfaceLight.copy(0.7f))
                            Icon(Icons.Default.ArrowForward, null, tint = SurfaceLight) }
                        Column(horizontalAlignment = Alignment.End) { Text(route.arrivalTime, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = SurfaceLight)
                            Text(route.destination, style = MaterialTheme.typography.bodyMedium, color = SurfaceLight.copy(0.8f)) }
                    }
                }
            }

                Column(Modifier.padding(20.dp)) {
                // Bus info
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Bus Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        route.bus.busNumber?.trim()?.takeIf { it.isNotEmpty() }?.let { num ->
                            Spacer(Modifier.height(4.dp))
                            Text("Bus no. $num", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            InfoChip(Icons.Default.AirlineSeatReclineExtra, route.bus.busType)
                            InfoChip(Icons.Default.EventSeat, "${route.availableSeats} seats")
                            InfoChip(Icons.Default.Star, "${route.bus.rating}")
                        }
                        if (route.bus.amenities.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Amenities", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                route.bus.amenities.take(4).forEach { SuggestionChip(onClick = {}, label = { Text(it, style = MaterialTheme.typography.labelSmall) }) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stops timeline
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Route Timetable", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        route.stops.forEachIndexed { index, stop ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(
                                        if (index == 0 || index == route.stops.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant))
                                    if (index < route.stops.lastIndex) Box(Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                                }
                                Column {
                                    Text(stop.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("Arr: ${stop.arrivalTime}  •  Dep: ${stop.departureTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    stop.layoverDuration?.let { Text("Layover: $it", style = MaterialTheme.typography.labelSmall, color = SecondaryOrange) }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Price & book
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Price per seat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${route.currency} ${"%.2f".format(route.price)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                    Button(onClick = { searchViewModel.loadSeats(routeId); onSelectBus() }, modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp)) {
                        Text("Select Seats", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
