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
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { formVisible = true }
    LaunchedEffect(uiState.isLoggedIn) { if (uiState.isLoggedIn) onLoginSuccess() }

    Box(Modifier.fillMaxSize()) {
        // Deep gradient background
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(PrimaryBlueDark, PrimaryBlue, Color(0xFF1B4F6A)))
            )
        )

        // Decorative blurred circles for depth
        Box(
            Modifier.size(320.dp).offset(x = (-80).dp, y = (-80).dp)
                .clip(CircleShape).background(PrimaryBlueLight.copy(alpha = 0.25f))
        )
        Box(
            Modifier.size(220.dp).align(Alignment.TopEnd).offset(x = 70.dp, y = 80.dp)
                .clip(CircleShape).background(GradientEnd.copy(alpha = 0.2f))
        )
        Box(
            Modifier.size(160.dp).align(Alignment.BottomStart).offset(x = (-40).dp, y = (-20).dp)
                .clip(CircleShape).background(AccentGreen.copy(alpha = 0.12f))
        )

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .statusBarsPadding().navigationBarsPadding()
        ) {
            // ── Hero ──────────────────────────────────────────────────────────
            Spacer(Modifier.height(52.dp))
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo ring
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
                            Icons.Default.DirectionsBus,
                            contentDescription = null,
                            Modifier.size(38.dp),
                            tint = SurfaceLight
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "NexiRide",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SurfaceLight,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your journey, simplified",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SurfaceLight.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(44.dp))

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
                            .padding(top = 36.dp, bottom = 32.dp)
                    ) {
                        Text(
                            "Welcome back",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Sign in to continue your journey",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(28.dp))

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, null, tint = PrimaryBlue)
                            },
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

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, null, tint = PrimaryBlue)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
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

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onNavigateToForgotPassword) {
                                Text(
                                    "Forgot Password?",
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Error banner
                        AnimatedVisibility(visible = uiState.error != null) {
                            uiState.error?.let { error ->
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
                                Spacer(Modifier.height(10.dp))
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Gradient sign-in button
                        val isEnabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
                        Box(
                            Modifier.fillMaxWidth().height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isEnabled)
                                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                                    else
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                )
                                .clickable(enabled = isEnabled) { authViewModel.login(email, password) },
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
                                        "Sign In",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnabled) SurfaceLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isEnabled) {
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            null,
                                            Modifier.size(18.dp),
                                            tint = SurfaceLight
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Divider
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                "or continue with",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Google button
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "G",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF4285F4)
                                )
                                Text(
                                    "Continue with Google",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Don't have an account?  ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Sign Up",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                modifier = Modifier.clickable { onNavigateToSignUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}
