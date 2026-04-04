package com.example.nexiride2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexiride2.ui.theme.*
import kotlinx.coroutines.delay

data class PromoBanner(val title: String, val subtitle: String, val code: String)

@Composable
fun PromoCarousel(banners: List<PromoBanner>, modifier: Modifier = Modifier) {
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(pagerState.settledPage) {
        delay(3500)
        val next = (pagerState.settledPage + 1) % banners.size
        pagerState.animateScrollToPage(next)
    }

    Column(modifier = modifier) {
        HorizontalPager(state = pagerState, contentPadding = PaddingValues(end = 32.dp), pageSpacing = 12.dp) { page ->
            val banner = banners[page]
            val gradient = if (page % 2 == 0) Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                else Brush.horizontalGradient(listOf(SecondaryOrange, StatusWarning))
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(140.dp)) {
                Box(Modifier.fillMaxSize().background(gradient).padding(20.dp)) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        Text(banner.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SurfaceLight)
                        Text(banner.subtitle, style = MaterialTheme.typography.bodySmall, color = SurfaceLight.copy(alpha = 0.8f))
                        Spacer(Modifier.height(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceLight.copy(alpha = 0.2f)) {
                            Text("Code: ${banner.code}", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SurfaceLight)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(banners.size) { i ->
                Box(Modifier.padding(horizontal = 3.dp).size(if (i == pagerState.currentPage) 20.dp else 6.dp, 6.dp)
                    .clip(if (i == pagerState.currentPage) RoundedCornerShape(3.dp) else CircleShape)
                    .background(if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
            }
        }
    }
}
