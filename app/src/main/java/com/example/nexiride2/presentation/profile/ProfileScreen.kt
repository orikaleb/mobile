package com.example.nexiride2.presentation.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexiride2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onNavigateToMyBookings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    var showEditSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showComingSoon by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showComingSoon) {
        showComingSoon?.let { snackbarHostState.showSnackbar("$it — Coming soon!"); showComingSoon = null }
    }
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            showEditSheet = false
            snackbarHostState.showSnackbar("Profile updated ✓")
            viewModel.clearUpdateSuccess()
        }
    }
    LaunchedEffect(uiState.walletMessage) {
        uiState.walletMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearWalletMessage()
        }
    }

    // ── Edit bottom sheet ─────────────────────────────────────────────────────
    if (showEditSheet) {
        EditProfileSheet(
            user = user,
            isLoading = uiState.isLoading,
            onDismiss = { showEditSheet = false },
            onSave = { n, e, p -> viewModel.updateProfile(n, e, p) }
        )
    }

    // ── About dialog ──────────────────────────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(Modifier.size(56.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsBus, null, Modifier.size(30.dp), tint = SurfaceLight)
                }
            },
            title = { Text("NexiRide", fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Intercity bus booking made simple.\nBuilt for Ghana 🇬🇭",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, lineHeight = 22.sp)
                }
            },
            confirmButton = {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                    .clickable { showAboutDialog = false },
                    contentAlignment = Alignment.Center) {
                    Text("Close", color = SurfaceLight, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Logout confirm ────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Log out?", fontWeight = FontWeight.Bold) },
            text = { Text("You'll need to sign in again to access your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background) { pad ->

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)) {

            // ── Avatar card ───────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .shadow(0.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 24.dp)
            ) {
                // Top gradient strip
                Box(Modifier.fillMaxWidth().height(100.dp)
                    .background(Brush.verticalGradient(listOf(PrimaryBlueDark, PrimaryBlue))))

                Column(
                    Modifier.fillMaxWidth().padding(top = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar ring
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            Modifier.size(88.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                user?.name?.take(1)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SurfaceLight
                            )
                        }
                        Box(
                            Modifier.size(26.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showEditSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(user?.name ?: "Loading…",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface)

                    Spacer(Modifier.height(2.dp))

                    Text(user?.phone ?: user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(10.dp))

                    // Tier chip
                    Surface(shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFB800).copy(alpha = 0.12f)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.Star, null, Modifier.size(13.dp), tint = Color(0xFFFFB800))
                            Text("Gold Traveller", style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold, color = Color(0xFFB8860B))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Account section ───────────────────────────────────────────────
            SettingsSection(title = "ACCOUNT") {
                SettingsRow(
                    icon = Icons.Default.Person,
                    iconBg = PrimaryBlue.copy(.1f),
                    iconTint = PrimaryBlue,
                    title = "Personal Information",
                    subtitle = user?.email ?: ""
                ) { showEditSheet = true }

                RowDivider()
            }

            WalletTopUpCard(
                balanceGhs = uiState.walletBalanceGhs,
                onAddFunds = { viewModel.topUpWallet(it) }
            )

            Spacer(Modifier.height(10.dp))

            // ── Trips section ─────────────────────────────────────────────────
            SettingsSection(title = "ACTIVITY") {
                SettingsRow(Icons.Default.ConfirmationNumber, PrimaryBlue.copy(.1f), PrimaryBlue,
                    "My Bookings", "View all your tickets", onNavigateToMyBookings)
                RowDivider()
                SettingsRow(Icons.Default.Route, Color(0xFF7C3AED).copy(.1f), Color(0xFF7C3AED),
                    "Saved Routes", "Quick-access favourite routes") { showComingSoon = "Saved Routes" }
                RowDivider()
                SettingsRow(Icons.Default.History, SecondaryOrange.copy(.1f), SecondaryOrange,
                    "Travel History", "Past trips & receipts", onNavigateToMyBookings)
            }

            Spacer(Modifier.height(10.dp))

            // ── Preferences section ───────────────────────────────────────────
            SettingsSection(title = "PREFERENCES") {
                SettingsRow(Icons.Default.Notifications, StatusWarning.copy(.12f), StatusWarning,
                    "Notifications", "Alerts & trip updates", onNavigateToNotifications)
                RowDivider()
                SettingsRow(Icons.Default.Language, StatusInfo.copy(.12f), StatusInfo,
                    "Language", "English (Ghana)") { showComingSoon = "Language" }
                RowDivider()
                SettingsRow(Icons.Default.DarkMode, MaterialTheme.colorScheme.onSurfaceVariant.copy(.1f),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    "Appearance", "System default") { showComingSoon = "Appearance" }
            }

            Spacer(Modifier.height(10.dp))

            // ── Support section ───────────────────────────────────────────────
            SettingsSection(title = "SUPPORT") {
                SettingsRow(Icons.Default.Security, AccentGreen.copy(.1f), AccentGreenDark,
                    "Privacy & Security", "Manage your data") { showComingSoon = "Privacy & Security" }
                RowDivider()
                SettingsRow(Icons.Default.Help, GradientEnd.copy(.1f), GradientEnd,
                    "Help Center", "FAQs & live support") { showComingSoon = "Help Center" }
                RowDivider()
                SettingsRow(Icons.Default.Info, PrimaryBlueLight.copy(.1f), PrimaryBlueLight,
                    "About NexiRide", "v1.0.0 · Legal") { showAboutDialog = true }
            }

            Spacer(Modifier.height(20.dp))

            // ── Logout ────────────────────────────────────────────────────────
            Box(Modifier.padding(horizontal = 16.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StatusError.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth().clickable { showLogoutDialog = true }
                ) {
                    Row(Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Logout, null, Modifier.size(18.dp), tint = StatusError)
                        Spacer(Modifier.width(8.dp))
                        Text("Log Out", style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold, color = StatusError)
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

// ── Edit Profile Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    user: com.example.nexiride2.domain.model.User?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name  by remember(user) { mutableStateOf(user?.name  ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var phone by remember(user) { mutableStateOf(user?.phone ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).navigationBarsPadding()) {
            // Handle
            Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
                .align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(20.dp))

            Text("Edit Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("Update your personal information",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            // Fields
            listOf(
                Triple(Icons.Default.Person,  "Full Name",     name)  to { v: String -> name  = v },
                Triple(Icons.Default.Email,   "Email Address", email) to { v: String -> email = v },
                Triple(Icons.Default.Phone,   "Phone Number",  phone) to { v: String -> phone = v }
            ).forEach { (triple, onChange) ->
                val (icon, label, value) = triple
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    label = { Text(label) },
                    leadingIcon = { Icon(icon, null, tint = PrimaryBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = if (label == "Phone Number")
                        KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor  = PrimaryBlue,
                        focusedLeadingIconColor = PrimaryBlue
                    )
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Cancel") }

                Box(
                    Modifier.weight(1f).height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                        .clickable(enabled = !isLoading) { onSave(name, email, phone) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = SurfaceLight, strokeWidth = 2.dp)
                    } else {
                        Text("Save Changes", color = SurfaceLight, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp)
        Surface(
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column { content() }
        }
    }
}

// ── Single settings row ───────────────────────────────────────────────────────

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(19.dp), tint = iconTint)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank())
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 68.dp), thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun WalletTopUpCard(balanceGhs: Double?, onAddFunds: (String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = AccentGreenDark)
                Text("Wallet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            if (balanceGhs != null) {
                Text("Balance: GHS ${"%.2f".format(balanceGhs)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            } else {
                Text("Sign in to use your wallet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "For trying the app — not real money.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount (GHS)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = balanceGhs != null
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onAddFunds(amountText); amountText = "" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = balanceGhs != null && amountText.isNotBlank()
            ) {
                Text("Add to wallet")
            }
        }
    }
}

