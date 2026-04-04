package com.example.nexiride2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    priceRange: ClosedFloatingPointRange<Float>,
    onPriceRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    selectedTimeFilter: String,
    onTimeFilterChange: (String) -> Unit,
    selectedCompany: String,
    onCompanyChange: (String) -> Unit,
    companies: List<String>,
    onApply: () -> Unit
) {
    if (!isVisible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding()) {
            Text("Filter Results", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            Text("Price Range", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            RangeSlider(value = priceRange, onValueChange = onPriceRangeChange, valueRange = 20f..500f, steps = 9)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("GHS ${"%.0f".format(priceRange.start)}", style = MaterialTheme.typography.bodySmall)
                Text("GHS ${"%.0f".format(priceRange.endInclusive)}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Text("Departure Time", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Any", "Morning", "Afternoon", "Evening").forEach { time ->
                    FilterChip(selected = selectedTimeFilter == time, onClick = { onTimeFilterChange(time) },
                        label = { Text(time) })
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Bus Company", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("All") + companies).forEach { company ->
                    FilterChip(selected = selectedCompany == company, onClick = { onCompanyChange(company) },
                        label = { Text(company, maxLines = 1) })
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Apply Filters") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
