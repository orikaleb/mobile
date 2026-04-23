package com.example.nexiride2.presentation.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.nexiride2.domain.model.Driver
import com.example.nexiride2.presentation.search.CityCoordinates
import com.example.nexiride2.ui.theme.*

/** Tab: pick between sign-in (existing driver) and sign-up (new registration). */
private enum class Tab { SignIn, SignUp }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverPortalScreen(
    viewModel: DriverAuthViewModel,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Trigger navigation once sign-in / sign-up succeeds. We consume the flag
    // to avoid re-navigating on configuration changes.
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            viewModel.consumeSignedIn()
            onAuthenticated()
        }
    }

    var tab by remember { mutableStateOf(Tab.SignIn) }

    // Shared form state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Sign-up-only state
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var busType by remember { mutableStateOf(Driver.BUS_TYPES.first()) }
    var busCapacityText by remember { mutableStateOf("") }
    // Stations the driver serves — multi-select from the known Ghana cities.
    val serviceStations = remember { mutableStateListOf<String>() }
    val supportedCities = remember { CityCoordinates.names.sorted() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(PrimaryBlueDark, PrimaryBlue, PrimaryBlueDark))
            )
        )

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .statusBarsPadding().navigationBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = SurfaceLight)
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(84.dp).clip(CircleShape)
                        .background(SurfaceLight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsBus,
                        null,
                        tint = SurfaceLight,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Driver Portal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SurfaceLight
                )
                Text(
                    "Manage your trips and sign in on shift",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SurfaceLight.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(28.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    // Tab strip
                    TabRow(
                        selectedTabIndex = tab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = tab == Tab.SignIn,
                            onClick = { tab = Tab.SignIn; viewModel.clearError() },
                            text = { Text("Sign in") }
                        )
                        Tab(
                            selected = tab == Tab.SignUp,
                            onClick = { tab = Tab.SignUp; viewModel.clearError() },
                            text = { Text("Register") }
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    if (tab == Tab.SignUp) {
                        DriverField(
                            value = fullName,
                            onChange = { fullName = it },
                            label = "Full name",
                            leadingIcon = Icons.Default.Person
                        )
                        DriverField(
                            value = phone,
                            onChange = { phone = it },
                            label = "Phone number",
                            leadingIcon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone
                        )
                        DriverField(
                            value = licenseNumber,
                            onChange = { licenseNumber = it },
                            label = "Driver's license number",
                            leadingIcon = Icons.Default.Badge
                        )
                        DriverField(
                            value = companyName,
                            onChange = { companyName = it },
                            label = "Company / operator",
                            leadingIcon = Icons.Default.Business
                        )

                        // ── Bus type ─────────────────────────────────────────
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Bus type",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Driver.BUS_TYPES.forEach { option ->
                                FilterChip(
                                    selected = busType == option,
                                    onClick = { busType = option },
                                    label = { Text(option) },
                                    leadingIcon = {
                                        Icon(Icons.Default.DirectionsBus, null, Modifier.size(16.dp))
                                    }
                                )
                            }
                        }

                        // ── Capacity ─────────────────────────────────────────
                        DriverField(
                            value = busCapacityText,
                            onChange = { input ->
                                busCapacityText = input.filter { it.isDigit() }.take(3)
                            },
                            label = "Passenger capacity",
                            leadingIcon = Icons.Default.EventSeat,
                            keyboardType = KeyboardType.Number
                        )

                        // ── Stations served ─────────────────────────────────
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Stations served",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Pick every city this bus stops at.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        StationMultiSelect(
                            allCities = supportedCities,
                            selected = serviceStations
                        )
                    }

                    DriverField(
                        value = email,
                        onChange = { email = it },
                        label = "Work email",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    DriverField(
                        value = password,
                        onChange = { password = it },
                        label = "Password",
                        leadingIcon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    null
                                )
                            }
                        }
                    )

                    state.error?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusError.copy(alpha = 0.09f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, null, Modifier.size(16.dp), tint = StatusError)
                            Text(err, color = StatusError, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    val capacity = busCapacityText.toIntOrNull() ?: 0
                    val canSubmit = when (tab) {
                        Tab.SignIn -> !state.isLoading && email.isNotBlank() && password.length >= 6
                        Tab.SignUp -> !state.isLoading && fullName.isNotBlank() && phone.isNotBlank() &&
                            licenseNumber.isNotBlank() && email.isNotBlank() && password.length >= 6 &&
                            busType.isNotBlank() && capacity in 1..200 && serviceStations.isNotEmpty()
                    }

                    Button(
                        onClick = {
                            if (tab == Tab.SignIn) {
                                viewModel.signIn(email.trim(), password)
                            } else {
                                viewModel.signUp(
                                    fullName = fullName.trim(),
                                    email = email.trim(),
                                    phone = phone.trim(),
                                    licenseNumber = licenseNumber.trim(),
                                    companyName = companyName.trim(),
                                    busType = busType,
                                    busCapacity = capacity,
                                    serviceStations = serviceStations.toList(),
                                    password = password
                                )
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                Modifier.size(22.dp),
                                color = SurfaceLight,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                if (tab == Tab.SignIn) "Sign in" else "Create driver account",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Drivers must register with an authorised company email. " +
                            "Admins can toggle your account active status at any time.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Wrapping grid of selectable city chips. Tapping toggles membership in the
 * caller's [selected] list so the driver can pick every station they serve.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationMultiSelect(
    allCities: List<String>,
    selected: MutableList<String>
) {
    // Manual wrap so we don't need a separate FlowRow dependency.
    val rows = allCities.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { city ->
                    val isSelected = city in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selected.remove(city) else selected.add(city)
                        },
                        label = { Text(city) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad the last row so chips don't stretch when it has fewer than 3.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (selected.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            "${selected.size} station${if (selected.size == 1) "" else "s"} selected",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            leadingIcon = { Icon(leadingIcon, null, tint = PrimaryBlue) },
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                focusedLabelColor = PrimaryBlue,
                focusedLeadingIconColor = PrimaryBlue
            )
        )
    }
}
