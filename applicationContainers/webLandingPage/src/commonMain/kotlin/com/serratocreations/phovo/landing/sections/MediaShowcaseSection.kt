package com.serratocreations.phovo.landing.sections

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import phovo.applicationcontainers.weblandingpage.generated.resources.Res
import phovo.applicationcontainers.weblandingpage.generated.resources.android_screenshot
import phovo.applicationcontainers.weblandingpage.generated.resources.desktop_screenshot
import phovo.applicationcontainers.weblandingpage.generated.resources.iphone_connections_screenshot
import phovo.applicationcontainers.weblandingpage.generated.resources.iphone_screenshot
import kotlin.math.absoluteValue

@Composable
fun MediaShowcaseSection() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        val isMobile = maxWidth < 768.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.widthIn(max = 1000.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "APPLICATION SHOWCASE",
                    color = Color(0xFF83D5C6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Sleek, Responsive UI Across Platforms",
                    color = Color.White,
                    fontSize = if (isMobile) 24.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isMobile) 32.sp else 42.sp
                )

                Text(
                    text = "Preview Phovo running natively on iOS, Android, and Desktop workstations.",
                    color = Color(0xFFBEC9C5),
                    fontSize = if (isMobile) 13.sp else 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isMobile) 18.sp else 22.sp
                )
            }

            DesktopShowcaseCard()
            MobileShowcaseCard(isMobile = isMobile)
        }
    }
}

private class PhoneScreenshot(
    val drawable: DrawableResource,
    val label: String,
    val contentDescription: String,
)

private val phoneScreenshots = listOf(
    PhoneScreenshot(Res.drawable.iphone_screenshot, "iOS · Photos", "iPhone photo timeline screenshot"),
    PhoneScreenshot(Res.drawable.iphone_connections_screenshot, "iOS · Connections", "iPhone connections screen screenshot"),
    PhoneScreenshot(Res.drawable.android_screenshot, "Android · Details", "Android media details screenshot"),
)

@Composable
private fun MobileShowcaseCard(isMobile: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141D1A)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3383D5C6)),
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        // No horizontal padding on mobile so the peeking carousel phones run to the card edge
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = if (isMobile) 0.dp else 20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xFF1B2925),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4483D5C6))
            ) {
                Text(
                    text = "Mobile Apps (iOS / Android)",
                    color = Color(0xFF9FF2E2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            if (isMobile) {
                PhoneCarousel()
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.widthIn(max = 780.dp).fillMaxWidth()
                ) {
                    phoneScreenshots.forEach { screenshot ->
                        PhoneScreenshotItem(screenshot = screenshot, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Looping auto-advancing carousel for narrow screens, with the neighbouring phones peeking in
@Composable
private fun PhoneCarousel() {
    val pageCount = phoneScreenshots.size
    // A large virtual page count lets the carousel keep moving forward and wrap around
    val virtualPageCount = pageCount * 1000
    val pagerState = rememberPagerState(initialPage = virtualPageCount / 2) { virtualPageCount }
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    // Restarts after each manual swipe, so auto-advance never fights the user's finger
    LaunchedEffect(isDragged) {
        if (isDragged) return@LaunchedEffect
        while (true) {
            delay(3000)
            pagerState.animateScrollToPage(
                page = pagerState.currentPage + 1,
                animationSpec = tween(durationMillis = 600)
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val pageWidth = minOf(maxWidth * 0.58f, 220.dp)
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = (maxWidth - pageWidth) / 2),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .absoluteValue
                    .coerceIn(0f, 1f)
                PhoneScreenshotItem(
                    screenshot = phoneScreenshots[page % pageCount],
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val scale = 1f - 0.12f * pageOffset
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.5f * pageOffset
                        }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pageCount) { index ->
                val isSelected = pagerState.currentPage % pageCount == index
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isSelected) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF83D5C6) else Color(0xFF3F4946))
                )
            }
        }
    }
}

@Composable
private fun PhoneScreenshotItem(screenshot: PhoneScreenshot, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Uncropped phone screenshot box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.46f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF090F0E))
                .border(2.dp, Color(0xFF3F4946), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(screenshot.drawable),
                contentDescription = screenshot.contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = screenshot.label,
            color = Color(0xFF899390),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DesktopShowcaseCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141D1A)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3383D5C6)),
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xFF1B2925),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4483D5C6))
            ) {
                Text(
                    text = "Desktop Client (macOS / Windows / Linux)",
                    color = Color(0xFF9FF2E2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Desktop Screenshot Rendered Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.61f)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090F0E))
                    .border(1.dp, Color(0xFF3F4946), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.desktop_screenshot),
                    contentDescription = "Desktop Screenshot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "Packaging: DMG, MSI, DEB Native Bundles",
                color = Color(0xFF899390),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
