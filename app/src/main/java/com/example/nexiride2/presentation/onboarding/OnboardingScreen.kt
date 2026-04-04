package com.example.nexiride2.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexiride2.ui.theme.*
import kotlinx.coroutines.launch

// ── Model ─────────────────────────────────────────────────────────────────────

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val accent: String,        // word(s) to highlight
    val description: String,
    val gradient: List<Color>,
    val accentColor: Color
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.Search,
        title = "Find Your\nPerfect Route",
        accent = "Perfect Route",
        description = "Browse hundreds of intercity bus routes across Ghana and compare prices in seconds.",
        gradient = listOf(PrimaryBlueDark, PrimaryBlue, Color(0xFF1B4F6A)),
        accentColor = GradientEnd
    ),
    OnboardingPage(
        icon = Icons.Default.EventSeat,
        title = "Choose\nYour Seat",
        accent = "Your Seat",
        description = "Pick the perfect spot with our live seat map and confirm your booking in just a few taps.",
        gradient = listOf(Color(0xFF0B2A3A), Color(0xFF0D4A58), Color(0xFF1A6A60)),
        accentColor = AccentGreen
    ),
    OnboardingPage(
        icon = Icons.Default.ConfirmationNumber,
        title = "Travel\nHassle-Free",
        accent = "Hassle-Free",
        description = "Your QR ticket is ready instantly. Track your bus live and manage everything from one app.",
        gradient = listOf(Color(0xFF180A35), Color(0xFF2A1860), PrimaryBlue),
        accentColor = SecondaryOrangeLight
    )
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit, onLogin: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val current = pagerState.currentPage
    val page = pages[current]

    // Smooth background color transition
    val bg0 by animateColorAsState(page.gradient[0], tween(600), label = "bg0")
    val bg1 by animateColorAsState(page.gradient[1], tween(600), label = "bg1")
    val bg2 by animateColorAsState(page.gradient[2], tween(600), label = "bg2")

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(bg0, bg1, bg2)))
    ) {
        // Subtle decorative circle top-right
        Box(
            Modifier.size(240.dp).align(Alignment.TopEnd).offset(x = 80.dp, y = (-60).dp)
                .clip(CircleShape)
                .background(page.accentColor.copy(alpha = 0.1f))
        )

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (current < pages.size - 1) {
                    TextButton(onClick = onGetStarted) {
                        Text("Skip", color = SurfaceLight.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                PageSlide(pages[index])
            }

            // Dot indicator
            Row(
                Modifier.padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { i ->
                    val isActive = i == current
                    val width by animateDpAsState(
                        if (isActive) 26.dp else 7.dp,
                        spring(Spring.DampingRatioMediumBouncy),
                        label = "dot"
                    )
                    val color by animateColorAsState(
                        if (isActive) page.accentColor else SurfaceLight.copy(alpha = 0.3f),
                        tween(300), label = "dot-color"
                    )
                    Box(Modifier.size(width, 7.dp).clip(RoundedCornerShape(4.dp)).background(color))
                }
            }

            // Buttons
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (current == pages.size - 1) {
                    // Get Started
                    Box(
                        Modifier.fillMaxWidth().height(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                            .clickable { onGetStarted() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Get Started", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SurfaceLight)
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp), tint = SurfaceLight)
                        }
                    }
                    // Already have account
                    Box(
                        Modifier.fillMaxWidth().height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, SurfaceLight.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .clickable { onLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "I already have an account",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = SurfaceLight.copy(alpha = 0.85f)
                        )
                    }
                } else {
                    // Next
                    Box(
                        Modifier.fillMaxWidth().height(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceLight.copy(alpha = 0.15f))
                            .border(1.dp, SurfaceLight.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                            .clickable { scope.launch { pagerState.animateScrollToPage(current + 1) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SurfaceLight)
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp), tint = SurfaceLight)
                        }
                    }
                }
            }
        }
    }
}

// ── Single page slide ─────────────────────────────────────────────────────────

@Composable
private fun PageSlide(page: OnboardingPage) {
    // Floating animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float-y"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with glowing rings
        Box(
            Modifier.offset(y = offsetY.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow ring
            Box(
                Modifier.size(180.dp).clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                page.accentColor.copy(alpha = glowAlpha * 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Mid ring
            Box(
                Modifier.size(130.dp).clip(CircleShape)
                    .background(SurfaceLight.copy(alpha = 0.08f))
            )
            // Icon circle
            Box(
                Modifier.size(96.dp).clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(GradientStart, page.accentColor))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(page.icon, null, Modifier.size(48.dp), tint = SurfaceLight)
            }
        }

        Spacer(Modifier.height(44.dp))

        // Title with accent-colored highlight
        val styled = buildAnnotatedString {
            val idx = page.title.indexOf(page.accent)
            if (idx >= 0) {
                append(page.title.substring(0, idx))
                withStyle(SpanStyle(color = page.accentColor, fontWeight = FontWeight.ExtraBold)) {
                    append(page.accent)
                }
                append(page.title.substring(idx + page.accent.length))
            } else {
                append(page.title)
            }
        }
        Text(
            styled,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = SurfaceLight,
            lineHeight = 42.sp,
            letterSpacing = (-0.5).sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = SurfaceLight.copy(alpha = 0.68f),
            lineHeight = 24.sp
        )
    }
}
