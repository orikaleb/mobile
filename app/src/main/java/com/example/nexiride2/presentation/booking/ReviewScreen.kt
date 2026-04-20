package com.example.nexiride2.presentation.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(route: Route?, bookingViewModel: BookingViewModel, onBack: () -> Unit, onPaymentSuccess: () -> Unit) {
    val uiState by bookingViewModel.uiState.collectAsState()
    val totalPrice = uiState.selectedSeats.size * (route?.price ?: 0.0)

    LaunchedEffect(route?.id, uiState.selectedSeats.size, route?.price) {
        bookingViewModel.setTripTotalGhs(totalPrice)
    }

    LaunchedEffect(Unit) {
        bookingViewModel.refreshWallet()
    }

    LaunchedEffect(uiState.booking) {
        if (uiState.booking != null) onPaymentSuccess()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Review trip") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Trip summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        if (route != null) {
                            SummaryRow("Route", "${route.origin} → ${route.destination}")
                            SummaryRow("Date", route.date)
                            SummaryRow("Time", "${route.departureTime} - ${route.arrivalTime}")
                            SummaryRow("Bus", route.bus.companyName)
                            SummaryRow("Duration", route.duration)
                        }
                        SummaryRow("Seats", uiState.selectedSeats.joinToString { it.number })
                        SummaryRow("Passengers", uiState.passengers.joinToString { it.name })
                        uiState.baggage.let { if (it.numberOfBags > 0) SummaryRow("Baggage", "${it.numberOfBags} bags (${it.totalWeight} kg)") }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = AccentGreen)
                            Text("Wallet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        val bal = uiState.walletBalanceGhs
                        if (bal != null) {
                            Text("Available: GHS ${"%.2f".format(bal)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Sign in to pay from your wallet.", style = MaterialTheme.typography.bodyMedium, color = StatusError)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Practice balance only — add money under Profile → Wallet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = StatusError, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "GHS ${"%.2f".format(totalPrice)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = { bookingViewModel.createBooking() },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading && uiState.walletBalanceGhs != null &&
                            (uiState.walletBalanceGhs ?: 0.0) >= totalPrice && totalPrice > 0
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.AccountBalanceWallet, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Pay from wallet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
