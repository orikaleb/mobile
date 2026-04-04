package com.example.nexiride2.presentation.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
    var selectedPayment by remember { mutableStateOf("MTN MoMo") }

    LaunchedEffect(uiState.booking) { if (uiState.booking != null) onPaymentSuccess() }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Review & Pay") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                // Trip summary
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Trip Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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

                // Payment method
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        listOf("MTN MoMo" to Icons.Default.PhoneAndroid, "Visa/Mastercard" to Icons.Default.CreditCard,
                            "Cash at Station" to Icons.Default.AttachMoney).forEach { (method, icon) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedPayment == method, onClick = { selectedPayment = method; bookingViewModel.updatePaymentMethod(method) })
                                Icon(icon, null, Modifier.size(20.dp).padding(start = 4.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(method, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                uiState.error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = StatusError) }
            }

            // Bottom pay bar
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("GHS ${"%.2f".format(totalPrice)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = { bookingViewModel.createBooking() }, modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp), enabled = !uiState.isLoading) {
                        if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else { Icon(Icons.Default.Payment, null); Spacer(Modifier.width(8.dp)); Text("Pay Now", fontWeight = FontWeight.Bold) }
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
