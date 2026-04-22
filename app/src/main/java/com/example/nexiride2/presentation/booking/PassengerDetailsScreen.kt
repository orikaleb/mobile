package com.example.nexiride2.presentation.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nexiride2.domain.model.BaggageInfo
import com.example.nexiride2.domain.model.Passenger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDetailsScreen(bookingViewModel: BookingViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val uiState by bookingViewModel.uiState.collectAsState()
    val seatCount = uiState.selectedSeats.size
    val currentUser = uiState.currentUser
    // Prefill passenger 1 with the signed-in user's name + phone.
    var passengers by remember(seatCount, currentUser?.id) {
        mutableStateOf(
            List(seatCount) { idx ->
                Passenger(
                    name = if (idx == 0) currentUser?.name.orEmpty() else "",
                    phone = if (idx == 0) currentUser?.phone.orEmpty() else "",
                    seatNumber = uiState.selectedSeats.getOrNull(idx)?.number ?: ""
                )
            }
        )
    }
    var bags by remember { mutableStateOf("1") }
    var weight by remember { mutableStateOf("15") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Passenger Details") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                passengers.forEachIndexed { index, passenger ->
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Passenger ${index + 1} — Seat ${passenger.seatNumber}",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = passenger.name, onValueChange = { name ->
                                passengers = passengers.toMutableList().also { it[index] = it[index].copy(name = name) } },
                                label = { Text("Full Name") }, leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = passenger.phone, onValueChange = { phone ->
                                passengers = passengers.toMutableList().also { it[index] = it[index].copy(phone = phone) } },
                                label = { Text("Phone Number") }, leadingIcon = { Icon(Icons.Default.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                        }
                    }
                }

                // Baggage
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Baggage Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = bags, onValueChange = { bags = it }, label = { Text("Bags") },
                                leadingIcon = { Icon(Icons.Default.Luggage, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
                            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") },
                                leadingIcon = { Icon(Icons.Default.FitnessCenter, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
                        }
                    }
                }
            }

            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Button(onClick = {
                    bookingViewModel.updatePassengers(passengers)
                    bookingViewModel.updateBaggage(BaggageInfo(bags.toIntOrNull() ?: 0, weight.toDoubleOrNull() ?: 0.0))
                    onContinue()
                }, modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding().height(56.dp),
                    shape = RoundedCornerShape(16.dp), enabled = passengers.all { it.name.isNotBlank() }) {
                    Text("Review Booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
