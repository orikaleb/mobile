package com.example.nexiride2.presentation.tracking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.nexiride2.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ── Realistic Accra → Kumasi route waypoints (N1 / N6 highway) ───────────────
private val routeWaypoints = listOf(
    LatLng(5.6037, -0.1870),   // Accra
    LatLng(5.6420, -0.2310),   // Achimota
    LatLng(5.7200, -0.3050),   // Ofankor
    LatLng(5.8080, -0.3617),   // Nsawam
    LatLng(5.9200, -0.4100),   // Aburi Hills
    LatLng(6.0427, -0.4531),   // Suhum
    LatLng(6.1300, -0.5200),   // Kukurantumi
    LatLng(6.1892, -0.5731),   // Apedwa
    LatLng(6.2766, -0.6498),   // Bunso Junction
    LatLng(6.3500, -0.7000),   // Anyinam
    LatLng(6.5576, -0.7617),   // Nkawkaw
    LatLng(6.6100, -0.9800),   // Juaso
    LatLng(6.6247, -1.2272),   // Konongo
    LatLng(6.6400, -1.4500),   // Obuasi Junction
    LatLng(6.6885, -1.6244)    // Kumasi
)

private val stopNames = listOf(
    "Accra (Tudu)", "Achimota", "Ofankor", "Nsawam",
    "Aburi Hills", "Suhum", "Kukurantumi", "Apedwa",
    "Bunso Jct", "Anyinam", "Nkawkaw", "Juaso",
    "Konongo", "Obuasi Jct", "Kumasi (Kejetia)"
)

// Interpolate smoothly between all waypoints → produces ~300 fine-grained points
private fun buildFullPath(waypoints: List<LatLng>, steps: Int = 20): List<LatLng> {
    val result = mutableListOf<LatLng>()
    for (i in 0 until waypoints.size - 1) {
        val a = waypoints[i]; val b = waypoints[i + 1]
        for (s in 0 until steps) {
            val t = s.toFloat() / steps
            result.add(LatLng(a.latitude + (b.latitude - a.latitude) * t,
                              a.longitude + (b.longitude - a.longitude) * t))
        }
    }
    result.add(waypoints.last())
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    title: String,
    onBack: () -> Unit,
    viewModel: LiveTrackingViewModel
) {
    val context = LocalContext.current
    val userLatLng by viewModel.userLocation.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) viewModel.startLocationUpdates()
    }
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) viewModel.startLocationUpdates()
        else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val fullPath  = remember { buildFullPath(routeWaypoints) }
    val totalPts  = fullPath.size

    // Progress: 0f = origin, 1f = destination
    // Start at 15% to show the bus already on its way
    var progress by remember { mutableFloatStateOf(0.15f) }

    // Move bus every 300 ms (1 step = ~1 km, feels real)
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(300)
            progress = (progress + 0.0018f).coerceAtMost(1f)
        }
    }

    // Current LatLng derived from progress
    val busIdx   = ((progress * (totalPts - 1)).toInt()).coerceIn(0, totalPts - 2)
    val localT   = (progress * (totalPts - 1)) - busIdx
    val busPt    = LatLng(
        fullPath[busIdx].latitude  + (fullPath[busIdx + 1].latitude  - fullPath[busIdx].latitude)  * localT,
        fullPath[busIdx].longitude + (fullPath[busIdx + 1].longitude - fullPath[busIdx].longitude) * localT
    )
    val distanceToBusKm = userLatLng?.let { haversineKm(it, busPt) }

    // Nearest named stop
    val waypointIdx = ((progress * (routeWaypoints.size - 1)).toInt()).coerceIn(0, routeWaypoints.size - 2)
    val nearestStop = stopNames.getOrElse(waypointIdx) { "" }
    val nextStop    = stopNames.getOrElse(waypointIdx + 1) { "Kumasi" }

    // Stats
    val totalKm      = 250
    val coveredKm    = (progress * totalKm).roundToInt()
    val remainingKm  = totalKm - coveredKm
    val etaMinutes   = (remainingKm / 0.833f).roundToInt()   // ~50 km/h avg
    val speedKmh     = (48 + (progress * 20).toInt())        // ramps up slightly
    val etaText      = if (etaMinutes >= 60) "${etaMinutes / 60}h ${etaMinutes % 60}m" else "${etaMinutes}m"

    val cameraState  = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(busPt, 9f)
    }

    // Camera follows bus
    LaunchedEffect(busPt) {
        cameraState.animate(
            CameraUpdateFactory.newLatLng(busPt),
            durationMs = 500
        )
    }

    // Pulsing live dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val pulse by infiniteTransition.animateFloat(
        1f, 1.6f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(Modifier.fillMaxSize()) {
        // ── Map ───────────────────────────────────────────────────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isTrafficEnabled = true),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            // Travelled portion (white)
            val travelledPath = fullPath.take(busIdx + 1)
            if (travelledPath.size >= 2) {
                Polyline(
                    points = travelledPath,
                    color  = Color.White.copy(alpha = 0.5f),
                    width  = 10f
                )
            }

            // Remaining route (brand blue)
            val remainingPath = fullPath.drop(busIdx)
            if (remainingPath.size >= 2) {
                Polyline(
                    points = remainingPath,
                    color  = PrimaryBlue,
                    width  = 12f
                )
            }

            // Origin marker
            Marker(
                state   = MarkerState(routeWaypoints.first()),
                title   = stopNames.first(),
                snippet = "Departed 07:30"
            )

            // Destination marker
            Marker(
                state   = MarkerState(routeWaypoints.last()),
                title   = stopNames.last(),
                snippet = "ETA in $etaText"
            )

            // Bus marker (live position)
            Marker(
                state   = MarkerState(busPt),
                title   = "Bus #GH-1234",
                snippet = "Near $nearestStop • ${speedKmh} km/h"
            )

            userLatLng?.let { u ->
                Marker(
                    state = MarkerState(u),
                    title = "Your location (GPS)",
                    snippet = distanceToBusKm?.let { d -> "~${"%.1f".format(d)} km to bus" } ?: "GPS fix"
                )
            }
        }

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button pill
            Surface(
                onClick = onBack,
                shape   = CircleShape,
                color   = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Route title pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.weight(1f))

            // LIVE badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StatusError,
                shadowElevation = 4.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        Modifier.size(7.dp).scale(pulse).clip(CircleShape).background(SurfaceLight)
                    )
                    Text("LIVE", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold, color = SurfaceLight)
                }
            }
        }

        // ── Bottom info card ─────────────────────────────────────────────────
        Surface(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 20.dp).navigationBarsPadding()
            ) {
                // Bus ID + speed row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsBus, null,
                                Modifier.size(20.dp), tint = PrimaryBlue)
                        }
                        Column {
                            Text("Bus #GH-1234", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text("Near $nearestStop",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$speedKmh km/h",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue)
                        Text("current speed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Progress bar
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stopNames.first(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text("${(progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue)
                        Text(stopNames.last(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(progress).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(GradientStart, GradientEnd)
                                    )
                                )
                        )
                        // Bus position dot on bar
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .align(Alignment.CenterStart)
                        ) {
                            Box(
                                Modifier.size(14.dp).align(Alignment.CenterEnd)
                                    .clip(CircleShape)
                                    .background(SurfaceLight)
                                    .then(Modifier.clip(CircleShape)
                                        .background(PrimaryBlue))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(Icons.Default.Route,      "$coveredKm km",    "Covered",   AccentGreen)
                    VerticalDivider(Modifier.height(36.dp))
                    StatItem(Icons.Default.Schedule,   etaText,            "ETA",       PrimaryBlue)
                    VerticalDivider(Modifier.height(36.dp))
                    StatItem(Icons.Default.LocationOn, "$remainingKm km",  "Remaining", SecondaryOrange)
                }

                Spacer(Modifier.height(14.dp))

                // Next stop banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryBlue.copy(alpha = 0.08f)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.NavigateNext, null,
                            Modifier.size(18.dp), tint = PrimaryBlue)
                        Text("Next stop: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(nextStop,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue)
                    }
                }

                distanceToBusKm?.let { km ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Straight-line distance from your GPS position to the bus: ~${"%.1f".format(km)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
