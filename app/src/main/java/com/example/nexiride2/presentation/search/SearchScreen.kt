package com.example.nexiride2.presentation.search

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.nexiride2.presentation.sensor.ShakeToRefresh
import com.example.nexiride2.ui.theme.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(searchViewModel: SearchViewModel, onSearchResults: () -> Unit) {
    val uiState by searchViewModel.uiState.collectAsState()
    var expanded1 by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val startOfTodayUtc = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    if (showDatePicker) {
        val initialMillis = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(uiState.date)?.time
        }.getOrNull() ?: startOfTodayUtc
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                            searchViewModel.updateDate(f.format(java.util.Date(ms)))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }

    LaunchedEffect(uiState.navigateToResults) {
        if (uiState.navigateToResults) {
            onSearchResults()
            searchViewModel.consumeNavigateToResults()
        }
    }

    ShakeToRefresh(
        enabled = uiState.origin.isNotBlank() && uiState.destination.isNotBlank() && !uiState.isLoading,
        onShake = { searchViewModel.search() }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.locationMessage) {
        uiState.locationMessage?.let {
            snackbarHostState.showSnackbar(it)
            searchViewModel.clearLocationMessage()
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLocationAndFillOrigin() {
        // Bail out early if the device's GPS/location master switch is off —
        // otherwise the fused provider silently returns coarse wifi/cell estimates
        // which are usually kilometers off.
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val gpsOn = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val networkOn = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        if (!gpsOn && !networkOn) {
            locating = false
            scope.launch {
                val act = snackbarHostState.showSnackbar(
                    message = "Location is turned off on this device",
                    actionLabel = "Settings"
                )
                if (act == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }
            return
        }

        locating = true
        val client = LocationServices.getFusedLocationProviderClient(context)

        scope.launch {
            val best = acquireBestLocation(client, timeoutMs = 10_000, targetAccuracyM = 30f)
            if (best == null) {
                locating = false
                searchViewModel.useCurrentLocation(0.0, 0.0) // out-of-Ghana branch → honest "couldn't locate"
                return@launch
            }
            val resolved = runCatching {
                reverseGeocode(context, best.latitude, best.longitude)
            }.getOrNull()
            locating = false
            val accuracy = if (best.hasAccuracy()) "±${best.accuracy.toInt()} m" else "unknown accuracy"
            val detectedAddress = buildString {
                append(resolved?.displayText ?: "${"%.4f".format(best.latitude)}, ${"%.4f".format(best.longitude)}")
                append(" · $accuracy")
            }
            searchViewModel.useCurrentLocation(
                lat = best.latitude,
                lng = best.longitude,
                detectedLocality = resolved?.locality,
                detectedAddress = detectedAddress
            )
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            fetchLocationAndFillOrigin()
        } else {
            locating = false
        }
    }

    val requestLocation: () -> Unit = {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            fetchLocationAndFillOrigin()
        } else {
            locating = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Search Buses", fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            // Use-current-location action
            AssistChip(
                onClick = requestLocation,
                enabled = !locating,
                label = {
                    Text(if (locating) "Locating…" else "Use my current location")
                },
                leadingIcon = {
                    if (locating) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.MyLocation, null, Modifier.size(16.dp))
                    }
                }
            )
            Spacer(Modifier.height(10.dp))

            // Origin
            ExposedDropdownMenuBox(expanded = expanded1, onExpandedChange = { expanded1 = it }) {
                OutlinedTextField(value = uiState.origin, onValueChange = { searchViewModel.updateOrigin(it) },
                    label = { Text("From") }, leadingIcon = { Icon(Icons.Default.TripOrigin, null, tint = AccentGreen) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded1) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), singleLine = true)
                ExposedDropdownMenu(expanded = expanded1, onDismissRequest = { expanded1 = false }) {
                    uiState.cities.filter { it.contains(uiState.origin, true) }.forEach { city ->
                        DropdownMenuItem(text = { Text(city) }, onClick = { searchViewModel.updateOrigin(city); expanded1 = false })
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            // Swap button
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { val o = uiState.origin; searchViewModel.updateOrigin(uiState.destination); searchViewModel.updateDestination(o) }) {
                    Icon(Icons.Default.SwapVert, "Swap", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(4.dp))

            // Destination
            ExposedDropdownMenuBox(expanded = expanded2, onExpandedChange = { expanded2 = it }) {
                OutlinedTextField(value = uiState.destination, onValueChange = { searchViewModel.updateDestination(it) },
                    label = { Text("To") }, leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = StatusError) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded2) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), singleLine = true)
                ExposedDropdownMenu(expanded = expanded2, onDismissRequest = { expanded2 = false }) {
                    uiState.cities.filter { it.contains(uiState.destination, true) }.forEach { city ->
                        DropdownMenuItem(text = { Text(city) }, onClick = { searchViewModel.updateDestination(city); expanded2 = false })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Date & Passengers
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.date,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Travel date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, null) } }
                )

                OutlinedTextField(value = uiState.passengers.toString(),
                    onValueChange = { it.toIntOrNull()?.let { p -> searchViewModel.updatePassengers(p.coerceIn(1, 10)) } },
                    label = { Text("Passengers") }, leadingIcon = { Icon(Icons.Default.People, null) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
            }

            uiState.error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = StatusError, style = MaterialTheme.typography.bodySmall) }
            uiState.cacheHint?.let { hint ->
                Spacer(Modifier.height(8.dp))
                Text(hint, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = { searchViewModel.search() }, modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading && uiState.origin.isNotBlank() && uiState.destination.isNotBlank()) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else { Icon(Icons.Default.Search, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text("Search Buses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(32.dp))
            // Quick routes
            Text("Quick Search", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Accra" to "Kumasi", "Accra" to "Cape Coast", "Kumasi" to "Tamale", "Accra" to "Tamale",
                "Accra" to "Sunyani", "Accra" to "Ho", "Kumasi" to "Cape Coast", "Accra" to "Wa"
            ).forEach { (from, to) ->
                Card(onClick = { searchViewModel.searchWithParams(from, to) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("$from → $to", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Live GPS fix acquisition
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Streams location updates from the fused provider for up to [timeoutMs] ms
 * and returns the most accurate fix seen. Stops early as soon as a fix at or
 * below [targetAccuracyM] meters arrives. Falls back to `lastLocation` if
 * nothing usable streamed in.
 *
 * Requires `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` to have been
 * granted already (caller should check).
 */
@SuppressLint("MissingPermission")
private suspend fun acquireBestLocation(
    client: FusedLocationProviderClient,
    timeoutMs: Long,
    targetAccuracyM: Float
): Location? {
    val best = kotlinx.coroutines.CompletableDeferred<Location?>()
    var bestSoFar: Location? = null

    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
        .setMinUpdateIntervalMillis(500L)
        .setWaitForAccurateLocation(true)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc in result.locations) {
                val prevAccuracy = bestSoFar?.takeIf { it.hasAccuracy() }?.accuracy ?: Float.MAX_VALUE
                if (loc.hasAccuracy() && loc.accuracy < prevAccuracy) {
                    bestSoFar = loc
                }
            }
            val current = bestSoFar
            if (current != null && current.hasAccuracy() && current.accuracy <= targetAccuracyM) {
                if (!best.isCompleted) best.complete(current)
            }
        }
    }

    try {
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (_: SecurityException) {
        return null
    }

    val streamed: Location? = withTimeoutOrNull(timeoutMs) { best.await() } ?: bestSoFar
    runCatching { client.removeLocationUpdates(callback) }
    if (streamed != null) return streamed

    // Fallback: whatever the OS last cached — better than nothing.
    return suspendCancellableCoroutine { cont ->
        try {
            client.lastLocation
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        } catch (_: SecurityException) {
            if (cont.isActive) cont.resume(null)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reverse-geocoding helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Most specific locality name we could resolve, plus a human-friendly address. */
private data class GeocodeResult(val locality: String?, val displayText: String?)

private fun Address.bestLocality(): String? =
    listOf(locality, subAdminArea, adminArea, countryName)
        .firstOrNull { !it.isNullOrBlank() }

private fun Address.summary(): String {
    val parts = listOfNotNull(
        locality?.takeIf { it.isNotBlank() },
        adminArea?.takeIf { it.isNotBlank() && it != locality },
        countryName?.takeIf { it.isNotBlank() && it != adminArea }
    )
    return parts.joinToString(", ")
}

/**
 * Reverse-geocodes [lat]/[lng] to a human-readable place on the IO dispatcher.
 * Works on both legacy (synchronous) and API 33+ (asynchronous) Geocoder APIs.
 */
private suspend fun reverseGeocode(
    context: android.content.Context,
    lat: Double,
    lng: Double
): GeocodeResult = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext GeocodeResult(null, null)
    val geocoder = Geocoder(context, Locale.getDefault())
    val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
            try {
                geocoder.getFromLocation(lat, lng, 1) { list ->
                    cont.resume(list.firstOrNull())
                }
            } catch (_: Exception) {
                cont.resume(null)
            }
        }
    } else {
        @Suppress("DEPRECATION")
        runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
    }
    if (address == null) GeocodeResult(null, null)
    else GeocodeResult(
        locality = address.bestLocality(),
        displayText = address.summary().ifBlank { address.bestLocality() }
    )
}
