package com.example.nexiride2.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexiride2.ui.components.BusResultCard
import com.example.nexiride2.ui.components.FilterBottomSheet
import com.example.nexiride2.ui.components.SkeletonBusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(searchViewModel: SearchViewModel, onBack: () -> Unit, onRouteSelected: (String) -> Unit) {
    val uiState by searchViewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { searchViewModel.consumeNavigateToResults() }
    var showFilter by remember { mutableStateOf(false) }
    var priceRange by remember { mutableStateOf(20f..500f) }
    var timeFilter by remember { mutableStateOf("Any") }
    var companyFilter by remember { mutableStateOf("All") }

    FilterBottomSheet(isVisible = showFilter, onDismiss = { showFilter = false },
        priceRange = priceRange, onPriceRangeChange = { priceRange = it },
        selectedTimeFilter = timeFilter, onTimeFilterChange = { timeFilter = it },
        selectedCompany = companyFilter, onCompanyChange = { companyFilter = it },
        companies = searchViewModel.getCompanies(),
        onApply = { searchViewModel.applyFilters(priceRange, timeFilter, companyFilter); showFilter = false })

    Scaffold(topBar = {
        TopAppBar(title = { Text("${uiState.origin} → ${uiState.destination}", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = { IconButton(onClick = { showFilter = true }) { Icon(Icons.Default.FilterList, "Filter") } })
    }) { padding ->
        if (uiState.isLoading) {
            LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)) {
                items(4) { SkeletonBusCard() }
            }
        } else if (uiState.filteredResults.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("No buses found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Try different dates or routes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${uiState.filteredResults.size} bus${if (uiState.filteredResults.size > 1) "es" else ""} found",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        uiState.cacheHint?.let { hint ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    hint,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
                items(uiState.filteredResults) { route ->
                    BusResultCard(route = route, onClick = { searchViewModel.selectRoute(route.id); onRouteSelected(route.id) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
