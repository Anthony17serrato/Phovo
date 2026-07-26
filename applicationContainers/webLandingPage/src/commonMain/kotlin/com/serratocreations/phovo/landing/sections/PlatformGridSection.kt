package com.serratocreations.phovo.landing.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PlatformInfo(
    val tag: String,
    val name: String,
    val osDetails: String,
    val status: String,
    val features: List<String>
)

@Composable
fun PlatformGridSection() {
    val platforms = listOf(
        PlatformInfo(
            tag = "macOS",
            name = "macOS",
            osDetails = "Apple Silicon & Intel (macOS 12+)",
            status = "Alpha Aug 31",
            features = listOf("Native Desktop UI", "Background Server Sync", "Multi-Window Support")
        ),
        PlatformInfo(
            tag = "Windows",
            name = "Windows",
            osDetails = "Windows 10 / 11 (64-bit)",
            status = "Alpha Aug 31",
            features = listOf("Tray Notifications", "Automatic Backup Engine", "High-DPI Display")
        ),
        PlatformInfo(
            tag = "Linux",
            name = "Linux",
            osDetails = "Ubuntu, Debian, Fedora & Arch",
            status = "Alpha Aug 31",
            features = listOf("Low Memory Footprint", "Headless Daemon Support", "Systemd Integration")
        ),
        PlatformInfo(
            tag = "Android",
            name = "Android",
            osDetails = "Android 7.0+ (API 24+)",
            status = "Alpha Aug 31",
            features = listOf("Auto Camera Roll Sync", "Background Uploads", "Material 3 Adaptive UI")
        ),
        PlatformInfo(
            tag = "iOS",
            name = "iOS",
            osDetails = "iOS 15.0+ (iPhone & iPad)",
            status = "Alpha Aug 31",
            features = listOf("Photos Library Integration", "Background App Refresh", "Native iOS Feel")
        )
    )

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
                    text = "CROSS-PLATFORM HARMONY",
                    color = Color(0xFF83D5C6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Built for Desktop, Android & iOS",
                    color = Color.White,
                    fontSize = if (isMobile) 24.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isMobile) 32.sp else 40.sp
                )

                Text(
                    text = "A single unified codebase powering seamless photo and video access on all your personal devices.",
                    color = Color(0xFFBEC9C5),
                    fontSize = if (isMobile) 13.sp else 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isMobile) 18.sp else 22.sp
                )
            }

            // Responsive Platform Cards Grid
            if (isMobile) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    platforms.forEach { platform ->
                        PlatformCard(platform)
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top Row: 3 Desktop targets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        platforms.take(3).forEach { platform ->
                            Box(modifier = Modifier.weight(1f)) {
                                PlatformCard(platform)
                            }
                        }
                    }

                    // Bottom Row: 2 Mobile targets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        platforms.drop(3).forEach { platform ->
                            Box(modifier = Modifier.weight(1f)) {
                                PlatformCard(platform)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformCard(platform: PlatformInfo) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141D1A)
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3383D5C6)),
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = platform.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = platform.osDetails,
                        color = Color(0xFF899390),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = Color(0x2283D5C6),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = platform.status,
                        color = Color(0xFF9FF2E2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0x223F4946))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                platform.features.forEach { feat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "•", color = Color(0xFF83D5C6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = feat, color = Color(0xFFBEC9C5), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
