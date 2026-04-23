package com.example.nexiride2.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexiride2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToDriverPortal: () -> Unit = {},
    onSignUpSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }

    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
    val isFormValid = !uiState.isLoading
            && name.isNotBlank()
            && email.isNotBlank()
            && password.length >= 4
            && password == confirmPassword

    LaunchedEffect(Unit) { formVisible = true }
    LaunchedEffect(uiState.isLoggedIn) { if (uiState.isLoggedIn) onSignUpSuccess() }

    Box(Modifier.fillMaxSize()) {
        // Background gradient — slightly different teal-to-blue angle vs Login
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D2B4A), PrimaryBlue, Color(0xFF1B5E6A))
                )
            )
        )

        // Decorative circles
        Box(
            Modifier.size(280.dp).align(Alignment.TopEnd).offset(x = 80.dp, y = (-60).dp)
                .clip(CircleShape).background(GradientEnd.copy(alpha = 0.22f))
        )
        Box(
            Modifier.size(200.dp).offset(x = (-60).dp, y = 100.dp)
                .clip(CircleShape).background(PrimaryBlueLight.copy(alpha = 0.18f))
        )
        Box(
            Modifier.size(140.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = (-40).dp)
                .clip(CircleShape).background(AccentGreen.copy(alpha = 0.1f))
        )

        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Hero ──────────────────────────────────────────────────────────
            Spacer(Modifier.height(40.dp))
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape)
                            .background(SurfaceLight.copy(alpha = 0.12f))
                    )
                    Box(
                        Modifier.size(72.dp).clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(SurfaceLight.copy(alpha = 0.3f), SurfaceLight.copy(alpha = 0.1f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            Modifier.size(36.dp),
                            tint = SurfaceLight
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Create Account",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SurfaceLight,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Join thousands of travellers",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SurfaceLight.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Form card ─────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .padding(top = 32.dp, bottom = 32.dp)
                    ) {
                        Text(
                            "Your details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Fill in the info below to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(24.dp))

                        // Step badges row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StepBadge(1, "Info", name.isNotBlank() && email.isNotBlank())
                            Box(Modifier.weight(1f).height(2.dp).background(
                                if (name.isNotBlank() && email.isNotBlank())
                                    Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                                else
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.outlineVariant,
                                            MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                            ))
                            StepBadge(2, "Security", password.length >= 4 && password == confirmPassword)
                        }

                        Spacer(Modifier.height(24.dp))

                        // Full name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                focusedLabelColor = PrimaryBlue,
                                focusedLeadingIconColor = PrimaryBlue
                            )
                        )

                        Spacer(Modifier.height(14.dp))

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email address") },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryBlue) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                focusedLabelColor = PrimaryBlue,
                                focusedLeadingIconColor = PrimaryBlue
                            )
                        )

                        Spacer(Modifier.height(14.dp))

                        // Phone
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = PrimaryBlue) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                focusedLabelColor = PrimaryBlue,
                                focusedLeadingIconColor = PrimaryBlue
                            )
                        )

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(20.dp))

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryBlue) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                focusedLabelColor = PrimaryBlue,
                                focusedLeadingIconColor = PrimaryBlue
                            )
                        )

                        // Password strength indicator
                        if (password.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            PasswordStrengthBar(password)
                        }

                        Spacer(Modifier.height(14.dp))

                        // Confirm password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = {
                                Icon(
                                    if (confirmPassword.isNotEmpty() && password == confirmPassword)
                                        Icons.Default.CheckCircle else Icons.Default.Lock,
                                    null,
                                    tint = if (confirmPassword.isNotEmpty() && password == confirmPassword)
                                        StatusSuccess else PrimaryBlue
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            isError = !passwordsMatch,
                            supportingText = if (!passwordsMatch) {
                                { Text("Passwords do not match", color = StatusError) }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (confirmPassword.isNotEmpty() && password == confirmPassword) StatusSuccess else PrimaryBlue,
                                focusedLabelColor = if (confirmPassword.isNotEmpty() && password == confirmPassword) StatusSuccess else PrimaryBlue,
                                focusedLeadingIconColor = PrimaryBlue,
                                errorBorderColor = StatusError,
                                errorLabelColor = StatusError
                            )
                        )

                        // Error banner
                        AnimatedVisibility(visible = uiState.error != null) {
                            uiState.error?.let { error ->
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
                                    Text(error, color = StatusError, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Gradient create account button
                        Box(
                            Modifier.fillMaxWidth().height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isFormValid)
                                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                                    else
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                )
                                .clickable(enabled = isFormValid) {
                                    authViewModel.signUp(name, email, phone, password)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    Modifier.size(24.dp),
                                    color = SurfaceLight,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Create Account",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFormValid) SurfaceLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isFormValid) {
                                        Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp), tint = SurfaceLight)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Terms note
                        Text(
                            "By creating an account you agree to our Terms of Service and Privacy Policy.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(24.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Already have an account?  ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Sign In",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                modifier = Modifier.clickable { onNavigateToLogin() }
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Driver portal shortcut — takes bus drivers straight to
                        // their dedicated sign-in / registration flow.
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToDriverPortal() }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                null,
                                Modifier.size(16.dp),
                                tint = PrimaryBlue
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Bus driver? Open the driver portal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBadge(number: Int, label: String, complete: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .background(
                    if (complete)
                        Brush.radialGradient(listOf(GradientStart, GradientEnd))
                    else
                        Brush.radialGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                )
                .then(
                    if (!complete) Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (complete) {
                Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = SurfaceLight)
            } else {
                Text(
                    "$number",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (complete) FontWeight.Bold else FontWeight.Normal,
            color = if (complete) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = when {
        password.length >= 10 && password.any { it.isUpperCase() } && password.any { it.isDigit() } -> 3
        password.length >= 7 -> 2
        else -> 1
    }
    val label = when (strength) {
        3 -> "Strong"
        2 -> "Medium"
        else -> "Weak"
    }
    val color = when (strength) {
        3 -> StatusSuccess
        2 -> StatusWarning
        else -> StatusError
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i < strength) color else MaterialTheme.colorScheme.outlineVariant)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
