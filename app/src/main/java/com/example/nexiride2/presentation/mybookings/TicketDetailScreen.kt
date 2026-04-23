package com.example.nexiride2.presentation.mybookings

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.nexiride2.domain.model.BookingStatus
import com.example.nexiride2.presentation.sensor.ProximityCoveredEffect
import com.example.nexiride2.ui.components.QrCodeView
import com.example.nexiride2.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    viewModel: MyBookingsViewModel,
    onBack: () -> Unit,
    onLiveTracking: (bookingId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val booking = uiState.selectedBooking ?: return
    var showCancelSheet by remember { mutableStateOf(false) }
    val downloaded by viewModel.downloadedTickets.collectAsState()
    val downloadedTicket = downloaded[booking.id]
    val context = LocalContext.current
    var proximityNear by remember { mutableStateOf(false) }
    val hasProximity = remember(context) {
        (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            .getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
    }
    if (hasProximity) {
        ProximityCoveredEffect { proximityNear = it }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.lastDownloadedTicket?.bookingId) {
        if (uiState.lastDownloadedTicket?.bookingId == booking.id) {
            snackbarHostState.showSnackbar("Ticket PDF saved for offline access.")
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showCancelSheet) {
        ModalBottomSheet(onDismissRequest = { showCancelSheet = false }) {
            Column(Modifier.padding(24.dp).navigationBarsPadding()) {
                Text("Cancellation Policy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("• Full refund if cancelled 24+ hours before departure\n• 50% refund if cancelled 12-24 hours before\n• No refund within 12 hours of departure",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { viewModel.cancelBooking(booking.id); showCancelSheet = false; onBack() },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)) {
                    Text("Confirm Cancellation", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showCancelSheet = false }, modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)) { Text("Keep Booking") }
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Ticket Details") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(GradientStart.copy(alpha = 0.92f), GradientEnd.copy(alpha = 0.85f))))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = when (booking.status) {
                            BookingStatus.CONFIRMED -> StatusSuccess
                            BookingStatus.CANCELLED -> StatusError
                            BookingStatus.PENDING -> StatusWarning
                            BookingStatus.COMPLETED -> StatusInfo
                        }.copy(alpha = 0.25f)
                    ) {
                        Text(
                            booking.status.name,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.Center) {
                        QrCodeView(data = booking.qrCodeData, size = 180.dp, showActions = !proximityNear)
                        if (proximityNear) {
                            Box(
                                Modifier
                                    .size(180.dp)
                                    .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Move the phone away to reveal your QR code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        booking.referenceCode,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "${booking.route.origin} → ${booking.route.destination}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(4.dp))
                    val bn = booking.route.bus.busNumber?.trim().orEmpty()
                    val type = booking.route.bus.busType
                    Text(
                        buildString {
                            if (bn.isNotEmpty()) append("Bus $bn · ")
                            append(type)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Trip Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    DetailRow("Route", "${booking.route.origin} → ${booking.route.destination}")
                    // Stations are the actual places the passenger boards / drops off.
                    // Fall back to the city name if we don't have a richer terminal entry.
                    val boardingStation = booking.route.stops.firstOrNull()?.name
                        ?.takeIf { it.isNotBlank() } ?: booking.route.origin
                    val arrivalStation = booking.route.stops.lastOrNull()?.name
                        ?.takeIf { it.isNotBlank() } ?: booking.route.destination
                    DetailRow("Boarding station", "$boardingStation, ${booking.route.origin}")
                    DetailRow("Drop-off station", "$arrivalStation, ${booking.route.destination}")
                    DetailRow("Date", booking.route.date)
                    DetailRow("Time", "${booking.route.departureTime} - ${booking.route.arrivalTime}")
                    DetailRow("Operator", booking.route.bus.companyName)
                    DetailRow(
                        "Bus number",
                        booking.route.bus.busNumber?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
                    )
                    DetailRow("Bus type", booking.route.bus.busType)
                    DetailRow("Seats", booking.seats.joinToString { it.number })
                    DetailRow("Passengers", booking.passengers.joinToString { it.name })
                    DetailRow("Paid with", booking.paymentMethod)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${booking.currency} ${"%.2f".format(booking.totalPrice)}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.downloadTicketPdf(booking.id)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(8.dp))
                Text(if (downloadedTicket != null) "Re-download PDF" else "Download PDF", fontWeight = FontWeight.Bold)
            }

            if (downloadedTicket != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val file = File(downloadedTicket.pdfPath)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { context.startActivity(intent) }.onFailure {
                            scope.launch {
                                snackbarHostState.showSnackbar("No PDF viewer found. File saved in app storage.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open PDF", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onLiveTracking(booking.id) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Map, null)
                Spacer(Modifier.width(8.dp))
                Text("Live Tracking", fontWeight = FontWeight.Bold)
            }

            if (booking.status == BookingStatus.CONFIRMED) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { showCancelSheet = true }, modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)) {
                    Icon(Icons.Default.Cancel, null); Spacer(Modifier.width(8.dp)); Text("Cancel Booking", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
