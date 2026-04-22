package com.example.nexiride2.presentation.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.nexiride2.ui.components.NotificationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            actions = {
                if (uiState.unreadCount > 0) {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Icon(Icons.Default.DoneAll, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Read all")
                    }
                }
            })
    }) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Couldn't load notifications", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.load() }) { Text("Retry") }
                }
            }
        } else if (uiState.notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("No notifications yet", style = MaterialTheme.typography.titleMedium)
                    Text("You're all caught up!", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(uiState.notifications) { notification ->
                    NotificationItem(notification = notification, onClick = { viewModel.markAsRead(notification.id) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
