package com.example.nexiride2.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexiride2.domain.model.BookingStatus
import com.example.nexiride2.domain.repository.AdminBookingEntry
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    var confirmCancelId by remember { mutableStateOf<String?>(null) }
    confirmCancelId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmCancelId = null },
            title = { Text("Cancel booking?") },
            text = {
                Text(
                    "This will free up the seats and mark booking $id as cancelled. " +
                        "The customer will be able to see the new status in their tickets."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelBookingAsAdmin(id)
                    confirmCancelId = null
                }) { Text("Cancel booking", color = StatusError) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelId = null }) { Text("Keep") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Console", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                AdminTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                return@Scaffold
            }

            when (uiState.selectedTab) {
                AdminTab.OVERVIEW -> OverviewTab(
                    state = uiState,
                    onReseed = { viewModel.reseedData() }
                )
                AdminTab.USERS -> UsersTab(users = uiState.users)
                AdminTab.BOOKINGS -> BookingsTab(
                    entries = uiState.bookings,
                    cancellingBookingId = uiState.cancellingBookingId,
                    onRequestCancel = { confirmCancelId = it }
                )
                AdminTab.BROADCAST -> BroadcastTab(
                    title = uiState.broadcastTitle,
                    message = uiState.broadcastMessage,
                    isSending = uiState.isBroadcasting,
                    recipients = uiState.users.size,
                    onTitleChange = viewModel::updateBroadcastTitle,
                    onMessageChange = viewModel::updateBroadcastMessage,
                    onSend = { viewModel.sendBroadcast() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    state: AdminUiState,
    onReseed: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { AdminHeaderCard(name = state.currentUser?.name, email = state.currentUser?.email) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Users", state.users.size.toString(), PrimaryBlue)
                StatCard(Modifier.weight(1f), "Bookings", state.bookings.size.toString(), AccentGreenDark)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Cities", state.cities.size.toString(), SecondaryOrange)
                StatCard(
                    Modifier.weight(1f),
                    "Revenue (GHS)",
                    "%.0f".format(state.totalRevenueGhs),
                    PrimaryBlueLight
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Data management",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Seed the Firestore routes/cities collections from the bundled mock data when they are empty.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onReseed,
                        enabled = !state.isSeeding,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSeeding) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Run seed check", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Top routes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        items(state.popularRoutes) { route ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${route.origin} → ${route.destination}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${route.bus.companyName} • ${route.departureTime} • ${route.duration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "GHS ${"%.0f".format(route.price)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Users
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UsersTab(users: List<User>) {
    if (users.isEmpty()) {
        EmptyState(icon = Icons.Default.GroupOff, text = "No users visible yet")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "${users.size} user(s)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(users, key = { it.id }) { user ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.name.take(1).uppercase().ifBlank { "?" },
                            color = SurfaceLight,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            user.name.ifBlank { "Unnamed" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (user.email.isNotBlank()) {
                            Text(
                                user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        if (user.phone.isNotBlank()) {
                            Text(
                                user.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "UID: ${user.id.take(10)}…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bookings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BookingsTab(
    entries: List<AdminBookingEntry>,
    cancellingBookingId: String?,
    onRequestCancel: (String) -> Unit
) {
    if (entries.isEmpty()) {
        EmptyState(icon = Icons.Default.ReceiptLong, text = "No bookings yet")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "${entries.size} booking(s)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(entries, key = { it.booking.id }) { entry ->
            val booking = entry.booking
            val isCancelling = cancellingBookingId == booking.id
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${booking.route.origin} → ${booking.route.destination}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Ref ${booking.referenceCode} • ${booking.bookingDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "User ${entry.userId.take(10)}… • ${booking.seats.size} seat(s) • GHS ${"%.0f".format(booking.totalPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(booking.status)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (booking.status != BookingStatus.CANCELLED) {
                        OutlinedButton(
                            onClick = { onRequestCancel(booking.id) },
                            enabled = !isCancelling,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                        ) {
                            if (isCancelling) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = StatusError
                                )
                            } else {
                                Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cancel booking", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BookingStatus) {
    val (bg, fg) = when (status) {
        BookingStatus.CONFIRMED -> AccentGreenDark.copy(alpha = 0.15f) to AccentGreenDark
        BookingStatus.PENDING -> StatusWarning.copy(alpha = 0.15f) to Color(0xFFB8860B)
        BookingStatus.CANCELLED -> StatusError.copy(alpha = 0.15f) to StatusError
        BookingStatus.COMPLETED -> PrimaryBlue.copy(alpha = 0.15f) to PrimaryBlue
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            status.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Broadcast
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BroadcastTab(
    title: String,
    message: String,
    isSending: Boolean,
    recipients: Int,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Send push-style notification",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Writes a notification doc for every user in Firestore. They'll see it in their Notifications tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            leadingIcon = { Icon(Icons.Default.Title, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("Message") },
            leadingIcon = { Icon(Icons.Default.Campaign, null) },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 6
        )

        Button(
            onClick = onSend,
            enabled = !isSending && title.isNotBlank() && message.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Broadcast to $recipients user(s)", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared bits
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AdminHeaderCard(name: String?, email: String?) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(SurfaceLight.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.AdminPanelSettings, null, tint = SurfaceLight) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Signed in as",
                        color = SurfaceLight.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        name ?: "Admin",
                        color = SurfaceLight,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!email.isNullOrBlank()) {
                        Text(
                            email,
                            color = SurfaceLight.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = tint
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                null,
                Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
