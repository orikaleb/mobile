package com.example.nexiride2.presentation.search

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexiride2.presentation.sensor.ShakeToRefresh
import com.example.nexiride2.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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

    Scaffold(topBar = { TopAppBar(title = { Text("Search Buses", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
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
