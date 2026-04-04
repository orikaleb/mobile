package com.example.nexiride2.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nexiride2.ui.theme.StatusError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(authViewModel: AuthViewModel, onBack: () -> Unit, onResetSuccess: () -> Unit) {
    val uiState by authViewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState.passwordReset) { if (uiState.passwordReset) onResetSuccess() }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Reset Password") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Default.LockReset, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            when {
                !uiState.otpSent -> {
                    Text("Enter your email address and we'll send you a verification code.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { authViewModel.forgotPassword(email) }, modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp), enabled = !uiState.isLoading && email.isNotBlank()) {
                        if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Send Verification Code", fontWeight = FontWeight.Bold)
                    }
                }
                !uiState.otpVerified -> {
                    Text("Enter the 4-digit code sent to $email", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(value = otp, onValueChange = { if (it.length <= 4) otp = it }, label = { Text("OTP Code") },
                        leadingIcon = { Icon(Icons.Default.Pin, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    Text("Hint: Use 1234", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { authViewModel.verifyOtp(email, otp) }, modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp), enabled = !uiState.isLoading && otp.length == 4) {
                        if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Verify Code", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Text("Create your new password", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { authViewModel.resetPassword(email, newPassword) }, modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp), enabled = !uiState.isLoading && newPassword.length >= 4) {
                        if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Reset Password", fontWeight = FontWeight.Bold)
                    }
                }
            }
            uiState.error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = StatusError, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
