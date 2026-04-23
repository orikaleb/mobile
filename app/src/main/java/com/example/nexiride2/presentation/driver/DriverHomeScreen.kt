package com.example.nexiride2.presentation.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    viewModel: DriverHomeViewModel,
    onSignedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // If Firebase Auth flips to signed-out (logout, token expiry), bounce
    // the driver out of this screen back to the portal.
    LaunchedEffect(state.driver, state.isLoading) {
        if (!state.isLoading && state.driver == null) onSignedOut()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Portal", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, null)
                    }
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(Icons.Default.Logout, null)
                    }
                }
            )
        }
    ) { pad ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val driver = state.driver
        if (driver == null) {
            Column(
                Modifier.fillMaxSize().padding(pad).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = StatusError)
                Spacer(Modifier.height(12.dp))
                Text(state.error ?: "Not signed in.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { DriverHeroCard(driver.fullName, driver.companyName) }
            item { DriverInfoCard(driver) }

            item {
                Text(
                    "Today's trips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.myRoutes.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "No trips assigned yet.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Once the admin assigns a bus registration or confirms your " +
                                    "company, your routes will appear here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.myRoutes, key = { it.id }) { route -> DriverRouteCard(route) }
            }
        }
    }
}

@Composable
private fun DriverHeroCard(name: String, companyName: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(GradientStart, GradientEnd))
            ).padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(SurfaceLight.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DirectionsBus, null, tint = SurfaceLight)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Welcome back,",
                            style = MaterialTheme.typography.labelMedium,
                            color = SurfaceLight.copy(alpha = 0.85f)
                        )
                        Text(
                            name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SurfaceLight
                        )
                        if (companyName.isNotBlank()) {
                            Text(
                                companyName,
                                style = MaterialTheme.typography.bodySmall,
                                color = SurfaceLight.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverInfoCard(driver: com.example.nexiride2.domain.model.Driver) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "My credentials",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            InfoRow(Icons.Default.Badge, "License", driver.licenseNumber.ifBlank { "—" })
            InfoRow(Icons.Default.Email, "Email", driver.email)
            InfoRow(Icons.Default.Phone, "Phone", driver.phone.ifBlank { "—" })
            InfoRow(
                Icons.Default.DirectionsBus,
                "Bus type",
                driver.busType.ifBlank { "—" }
            )
            InfoRow(
                Icons.Default.EventSeat,
                "Capacity",
                if (driver.busCapacity > 0) "${driver.busCapacity} seats" else "—"
            )
            InfoRow(
                Icons.Default.DirectionsBus,
                "Assigned bus",
                driver.assignedBusNumber?.takeIf { it.isNotBlank() } ?: "Not yet assigned"
            )
            InfoRow(
                if (driver.active) Icons.Default.CheckCircle else Icons.Default.Block,
                "Status",
                if (driver.active) "Active" else "Disabled"
            )

            if (driver.serviceStations.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Stations served",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                StationChipRow(driver.serviceStations)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationChipRow(stations: List<String>) {
    val rows = stations.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { city ->
                    AssistChip(
                        onClick = {},
                        label = { Text(city) },
                        leadingIcon = { Icon(Icons.Default.Place, null, Modifier.size(14.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DriverRouteCard(route: Route) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "${route.origin} → ${route.destination}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val boardingStation = route.stops.firstOrNull()?.name
                ?.takeIf { it.isNotBlank() && it != route.origin }
            val arrivalStation = route.stops.lastOrNull()?.name
                ?.takeIf { it.isNotBlank() && it != route.destination }
            if (boardingStation != null || arrivalStation != null) {
                Text(
                    "${boardingStation ?: route.origin} → ${arrivalStation ?: route.destination}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip(Icons.Default.AccessTime, "${route.departureTime} → ${route.arrivalTime}")
                InfoChip(Icons.Default.EventSeat, "${route.availableSeats} seats")
            }
            val busNo = route.bus.busNumber?.trim().orEmpty()
            if (busNo.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bus $busNo · ${route.bus.companyName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
