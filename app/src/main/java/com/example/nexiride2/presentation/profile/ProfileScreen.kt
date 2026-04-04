package com.example.nexiride2.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexiride2.domain.model.PaymentType
import com.example.nexiride2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel(), onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user
    var isEditing by remember { mutableStateOf(false) }
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var phone by remember(user) { mutableStateOf(user?.phone ?: "") }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showComingSoon by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showComingSoon) {
        showComingSoon?.let {
            snackbarHostState.showSnackbar("$it — Coming soon!")
            showComingSoon = null
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Text("🚌", style = MaterialTheme.typography.displaySmall) },
            title = { Text("About NexiRide", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Search routes, book seats, and track your ride — all in one place.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile", fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(scaffoldPadding)) {
        // Profile header
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(GradientStart, GradientEnd))).statusBarsPadding().padding(24.dp),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(SurfaceLight.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, Modifier.size(48.dp), tint = SurfaceLight)
                }
                Spacer(Modifier.height(12.dp))
                Text(user?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SurfaceLight)
                Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = SurfaceLight.copy(alpha = 0.8f))
            }
        }

        Column(Modifier.padding(20.dp)) {
            // Edit Profile
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Personal Info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            if (isEditing) viewModel.updateProfile(name, email, phone)
                            isEditing = !isEditing
                        }) { Text(if (isEditing) "Save" else "Edit") }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (isEditing) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    } else {
                        ProfileInfoRow(Icons.Default.Person, "Name", user?.name ?: "")
                        ProfileInfoRow(Icons.Default.Email, "Email", user?.email ?: "")
                        ProfileInfoRow(Icons.Default.Phone, "Phone", user?.phone ?: "")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Payment methods
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Payment Methods", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    uiState.paymentMethods.forEach { pm ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                Icon(
                                    if (pm.type == PaymentType.MOBILE_MONEY_MTN) Icons.Default.PhoneAndroid else Icons.Default.CreditCard,
                                    null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(pm.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(pm.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (pm.isDefault) Surface(shape = RoundedCornerShape(6.dp), color = AccentGreen.copy(alpha = 0.15f)) {
                                    Text("Default", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                                }
                                if (!pm.isDefault) {
                                    IconButton(onClick = { viewModel.removePaymentMethod(pm.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.DeleteOutline, "Remove", Modifier.size(18.dp), tint = StatusError)
                                    }
                                }
                            }
                        }
                        if (pm != uiState.paymentMethods.last()) HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Menu items
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    val menuItems = listOf(
                        Triple(Icons.Default.Route, "Saved Routes", { showComingSoon = "Saved Routes" }),
                        Triple(Icons.Default.Help, "Help Center", { showComingSoon = "Help Center" }),
                        Triple(Icons.Default.Info, "About NexiRide", { showAboutDialog = true })
                    )
                    menuItems.forEach { (icon, label, action) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable { action() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log Out", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(100.dp))
        }
    }
    }
}

@Composable
private fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyMedium) }
    }
}
