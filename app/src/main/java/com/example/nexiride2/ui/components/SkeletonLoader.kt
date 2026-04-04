package com.example.nexiride2.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerEffect(modifier: Modifier = Modifier, widthDp: Dp = 200.dp, heightDp: Dp = 20.dp, cornerRadius: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "shimmer"
    )
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim.value - 200, 0f), end = Offset(translateAnim.value, 0f)
    )
    Box(modifier = modifier.size(widthDp, heightDp).clip(RoundedCornerShape(cornerRadius)).background(brush))
}

@Composable
fun SkeletonBusCard(modifier: Modifier = Modifier) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(surfaceColor).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ShimmerEffect(widthDp = 120.dp, heightDp = 16.dp)
            ShimmerEffect(widthDp = 60.dp, heightDp = 16.dp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ShimmerEffect(widthDp = 60.dp, heightDp = 28.dp)
            ShimmerEffect(widthDp = 80.dp, heightDp = 12.dp)
            ShimmerEffect(widthDp = 60.dp, heightDp = 28.dp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ShimmerEffect(widthDp = 100.dp, heightDp = 14.dp)
            ShimmerEffect(widthDp = 80.dp, heightDp = 24.dp)
        }
    }
}
