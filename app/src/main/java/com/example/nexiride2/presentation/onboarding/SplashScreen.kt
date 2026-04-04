package com.example.nexiride2.presentation.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexiride2.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    // ── Animation states ──────────────────────────────────────────────────────
    val bgAlpha       = remember { Animatable(0f) }
    val logoScale     = remember { Animatable(0.15f) }
    val logoAlpha     = remember { Animatable(0f) }
    val ringScale     = remember { Animatable(0.4f) }
    val ringAlpha     = remember { Animatable(0f) }
    val titleOffsetY  = remember { Animatable(28f) }
    val titleAlpha    = remember { Animatable(0f) }
    val taglineAlpha  = remember { Animatable(0f) }
    val shimmerOffset = remember { Animatable(-300f) }

    // Pulsing glow ring (infinite)
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse = infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow-pulse"
    )
    // Loading dots bounce
    val dot1 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(450, easing = EaseInOutSine), RepeatMode.Reverse, StartOffset(0)),
        label = "dot1"
    )
    val dot2 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(450, easing = EaseInOutSine), RepeatMode.Reverse, StartOffset(150)),
        label = "dot2"
    )
    val dot3 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(450, easing = EaseInOutSine), RepeatMode.Reverse, StartOffset(300)),
        label = "dot3"
    )
    val dotsAlpha = remember { Animatable(0f) }

    // ── Animation sequence ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        // Phase 1 – background fades in
        bgAlpha.animateTo(1f, tween(500))

        // Phase 2 – outer ring expands + logo pops in
        launch {
            ringAlpha.animateTo(1f, tween(400))
            ringScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        }
        launch {
            delay(100)
            logoAlpha.animateTo(1f, tween(350))
            logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }

        delay(500)

        // Phase 3 – title slides up
        launch { titleAlpha.animateTo(1f, tween(450)) }
        launch { titleOffsetY.animateTo(0f, tween(450, easing = EaseOutCubic)) }

        delay(250)

        // Phase 4 – tagline fades in + shimmer sweeps once
        launch { taglineAlpha.animateTo(1f, tween(400)) }
        launch {
            shimmerOffset.animateTo(400f, tween(900, easing = LinearEasing))
        }

        delay(350)

        // Phase 5 – loading dots appear
        dotsAlpha.animateTo(1f, tween(300))

        delay(1000)
        onSplashComplete()
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        Modifier.fillMaxSize().alpha(bgAlpha.value)
            .background(
                Brush.verticalGradient(
                    listOf(PrimaryBlueDark, PrimaryBlue, Color(0xFF1B4F6A))
                )
            )
    ) {
        // Decorative background circles
        Box(
            Modifier.size(360.dp).align(Alignment.TopEnd).offset(x = 100.dp, y = (-80).dp)
                .clip(CircleShape).background(GradientEnd.copy(alpha = 0.15f)).blur(60.dp)
        )
        Box(
            Modifier.size(260.dp).align(Alignment.BottomStart).offset(x = (-80).dp, y = 80.dp)
                .clip(CircleShape).background(AccentGreen.copy(alpha = 0.1f)).blur(50.dp)
        )
        Box(
            Modifier.size(200.dp).offset(x = (-40).dp, y = 140.dp)
                .clip(CircleShape).background(PrimaryBlueLight.copy(alpha = 0.15f)).blur(40.dp)
        )

        // ── Centre content ────────────────────────────────────────────────────
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glow ring + logo icon
            Box(contentAlignment = Alignment.Center) {
                // Outer pulsing ring
                Box(
                    Modifier.size(140.dp).scale(ringScale.value).alpha(ringAlpha.value * glowPulse.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    SurfaceLight.copy(alpha = 0.2f),
                                    SurfaceLight.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Middle ring
                Box(
                    Modifier.size(108.dp).scale(ringScale.value).alpha(ringAlpha.value)
                        .clip(CircleShape).background(SurfaceLight.copy(alpha = 0.12f))
                )
                // Inner gradient circle with icon
                Box(
                    Modifier.size(80.dp).scale(logoScale.value).alpha(logoAlpha.value)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(GradientStart, GradientEnd))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsBus,
                        contentDescription = null,
                        Modifier.size(42.dp),
                        tint = SurfaceLight
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // App name with shimmer sweep
            Box(contentAlignment = Alignment.Center) {
                // Base text
                Text(
                    "NexiRide",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SurfaceLight,
                    letterSpacing = (-1).sp,
                    modifier = Modifier
                        .offset(y = titleOffsetY.value.dp)
                        .alpha(titleAlpha.value)
                )
                // Shimmer overlay
                Box(
                    Modifier
                        .matchParentSize()
                        .offset(y = titleOffsetY.value.dp)
                        .alpha(titleAlpha.value * 0.6f)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SurfaceLight.copy(alpha = 0.6f),
                                    Color.Transparent
                                ),
                                start = androidx.compose.ui.geometry.Offset(shimmerOffset.value, 0f),
                                end = androidx.compose.ui.geometry.Offset(shimmerOffset.value + 160f, 0f)
                            )
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Tagline
            Text(
                "Travel Smart. Travel Easy.",
                style = MaterialTheme.typography.bodyLarge,
                color = SurfaceLight.copy(alpha = 0.75f),
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }

        // ── Bottom section ────────────────────────────────────────────────────
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
                .alpha(dotsAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bouncing dots loader
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1.value, dot2.value, dot3.value).forEachIndexed { index, offsetY ->
                    Box(
                        Modifier.size(8.dp).offset(y = offsetY.dp)
                            .clip(CircleShape)
                            .background(
                                when (index) {
                                    0 -> SurfaceLight.copy(alpha = 0.9f)
                                    1 -> AccentGreen.copy(alpha = 0.85f)
                                    else -> GradientEnd.copy(alpha = 0.8f)
                                }
                            )
                    )
                }
            }

            // Version tag
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceLight.copy(alpha = 0.1f)
            ) {
                Text(
                    "v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = SurfaceLight.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
