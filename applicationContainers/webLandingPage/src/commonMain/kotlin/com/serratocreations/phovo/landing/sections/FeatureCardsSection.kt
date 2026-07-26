package com.serratocreations.phovo.landing.sections

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

data class FeatureInfo(
    val badge: String,
    val title: String,
    val description: String,
    val highlight: String
)

@Composable
fun FeatureCardsSection() {
    val features = listOf(
        FeatureInfo(
            badge = "PRIVACY FIRST",
            title = "100% Self-Hosted",
            description = "Store photos and videos on your own hardware or server. Your private memories remain yours—no third-party cloud monitoring or unexpected subscription fees.",
            highlight = "Zero Vendor Lock-in"
        ),
        FeatureInfo(
            badge = "HIGH SPEED",
            title = "Instant Device Sync",
            description = "Automatic background media upload from mobile devices to your self-hosted desktop storage. Optimized for quick indexing and instant thumbnail previews.",
            highlight = "Local Network Speed"
        ),
        FeatureInfo(
            badge = "MODERN ARCHITECTURE",
            title = "Kotlin Multiplatform",
            description = "Built using Compose Multiplatform UI for pixel-perfect, native UI execution on macOS, Linux, Windows, Android, and iOS from a single robust codebase.",
            highlight = "Native UI Performance"
        ),
        FeatureInfo(
            badge = "MEDIA ENGINE",
            title = "Full Resolution Storage",
            description = "Supports original high-resolution RAW photos, 4K/8K video playback, metadata extraction, and organized album creation across all connected devices.",
            highlight = "Lossless Backup"
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
                    text = "WHY PHOVO?",
                    color = Color(0xFF83D5C6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Designed for Privacy, Built for Speed",
                    color = Color.White,
                    fontSize = if (isMobile) 24.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isMobile) 32.sp else 40.sp
                )
            }

            // Grid of 4 Feature Cards (Responsive)
            if (isMobile) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    features.forEach { feature ->
                        FeatureCard(feature)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        features.take(2).forEach { feature ->
                            Box(modifier = Modifier.weight(1f)) {
                                FeatureCard(feature)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        features.drop(2).forEach { feature ->
                            Box(modifier = Modifier.weight(1f)) {
                                FeatureCard(feature)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCard(feature: FeatureInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141D1A)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3383D5C6)),
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = Color(0x2283D5C6),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = feature.badge,
                    color = Color(0xFF83D5C6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Text(
                text = feature.title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = feature.description,
                color = Color(0xFFBEC9C5),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Surface(
                color = Color(0x22006B5F),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = feature.highlight,
                    color = Color(0xFF9FF2E2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
