package com.example.nexiride2.presentation.mybookings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexiride2.ui.components.BookingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(viewModel: MyBookingsViewModel = hiltViewModel(), onBookingClick: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val downloaded by viewModel.downloadedTickets.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Past", "Cancelled")

    Scaffold(topBar = { TopAppBar(title = { Text("My Bookings", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) }) }
            }

            val bookings = when (selectedTab) { 0 -> uiState.upcoming; 1 -> uiState.past; else -> uiState.cancelled }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (bookings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎫", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("No ${tabs[selectedTab].lowercase()} bookings", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(bookings) { booking ->
                        BookingCard(
                            booking = booking,
                            isDownloaded = downloaded.containsKey(booking.id),
                            onClick = { viewModel.selectBooking(booking); onBookingClick(booking.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
