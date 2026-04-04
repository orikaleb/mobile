package com.example.nexiride2.presentation.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.model.SeatStatus
import com.example.nexiride2.ui.components.SeatMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(seats: List<Seat>, bookingViewModel: BookingViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val uiState by bookingViewModel.uiState.collectAsState()
    val displaySeats = seats.map { seat ->
        if (uiState.selectedSeats.any { it.id == seat.id }) seat.copy(status = SeatStatus.SELECTED) else seat
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Choose Seats") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("Tap to select your seats", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                SeatMapView(seats = displaySeats, onSeatClick = { bookingViewModel.toggleSeat(it) })
            }

            // Bottom bar
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column {
                        Text("${uiState.selectedSeats.size} seat${if (uiState.selectedSeats.size != 1) "s" else ""} selected",
                            style = MaterialTheme.typography.bodyMedium)
                        if (uiState.selectedSeats.isNotEmpty())
                            Text("Seats: ${uiState.selectedSeats.joinToString { it.number }}",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onContinue, enabled = uiState.selectedSeats.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp)) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
