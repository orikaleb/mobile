package com.example.nexiride2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.model.SeatStatus
import com.example.nexiride2.ui.theme.*

@Composable
fun SeatMapView(seats: List<Seat>, onSeatClick: (Seat) -> Unit, modifier: Modifier = Modifier) {
    val cols = 4
    val rows = if (seats.isEmpty()) 0 else seats.maxOf { it.row }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Legend
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SeatLegendItem(color = SeatAvailable, label = "Available")
            SeatLegendItem(color = SeatSelected, label = "Selected")
            SeatLegendItem(color = SeatReserved, label = "Reserved")
        }

        // Driver area
        Surface(Modifier.fillMaxWidth(0.6f).padding(bottom = 16.dp), shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("DRIVER", Modifier.padding(8.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Seat grid
        for (row in 1..rows) {
            Row(Modifier.fillMaxWidth(0.7f), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0 until cols) {
                    val seat = seats.find { it.row == row && it.column == col }
                    if (seat != null) {
                        SeatItem(seat = seat, onClick = { if (seat.status != SeatStatus.RESERVED) onSeatClick(seat) })
                    } else {
                        Spacer(Modifier.size(44.dp))
                    }
                    if (col == 1) Spacer(Modifier.width(24.dp)) // Aisle
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SeatItem(seat: Seat, onClick: () -> Unit) {
    val bgColor = when (seat.status) {
        SeatStatus.AVAILABLE -> SeatAvailable.copy(alpha = 0.15f)
        SeatStatus.RESERVED -> SeatReserved.copy(alpha = 0.15f)
        SeatStatus.SELECTED -> SeatSelected.copy(alpha = 0.2f)
    }
    val borderColor = when (seat.status) {
        SeatStatus.AVAILABLE -> SeatAvailable
        SeatStatus.RESERVED -> SeatReserved
        SeatStatus.SELECTED -> SeatSelected
    }
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = seat.status != SeatStatus.RESERVED, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(seat.number, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = borderColor)
    }
}

@Composable
private fun SeatLegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.3f)).border(1.5.dp, color, RoundedCornerShape(4.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
