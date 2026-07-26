package com.serratocreations.phovo.landing.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import phovo.applicationcontainers.weblandingpage.generated.resources.Res
import phovo.applicationcontainers.weblandingpage.generated.resources.phovo_transparent_icon

@Composable
fun HeroSection(
    onExploreClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        val isMobile = maxWidth < 768.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.widthIn(max = 840.dp)
        ) {
            // Large Phovo Brand Logo Showcase (Doubled size: 288.dp desktop / 216.dp mobile)
            Image(
                painter = painterResource(Res.drawable.phovo_transparent_icon),
                contentDescription = "Phovo App Icon",
                modifier = Modifier.size(if (isMobile) 216.dp else 288.dp)
            )

            // Status Pill
            Surface(
                color = Color(0xFF1B2925),
                shape = RoundedCornerShape(30.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF83D5C6))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(
                        horizontal = if (isMobile) 12.dp else 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF83D5C6))
                    )
                    Text(
                        text = "COMING SOON - Alpha builds starting August 31, 2026",
                        color = Color(0xFF9FF2E2),
                        fontSize = if (isMobile) 11.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Headline
            Text(
                text = "Your Memories. Your Server.\nTotal Ownership.",
                color = Color.White,
                fontSize = if (isMobile) 28.sp else 44.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = if (isMobile) 36.sp else 52.sp,
                letterSpacing = (-0.5).sp
            )

            // Subtitle
            Text(
                text = "Phovo is an open-source, self-hosted photo & video cloud storage solution built with Kotlin Multiplatform. Seamlessly access, back up, and organize your media across Desktop, Android, and iOS without third-party subscriptions.",
                color = Color(0xFFBEC9C5),
                fontSize = if (isMobile) 14.sp else 17.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = if (isMobile) 22.sp else 26.sp,
                modifier = Modifier.widthIn(max = 680.dp)
            )

            // CTA Button
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006B5F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Explore Features",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Highlighting key target platforms preview banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF172320), Color(0xFF0E1513))
                        )
                    )
                    .border(1.dp, Color(0x2283D5C6), RoundedCornerShape(20.dp))
                    .padding(if (isMobile) 16.dp else 24.dp)
            ) {
                if (isMobile) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PlatformPill("Desktop", "macOS, Windows, Linux")
                        PlatformPill("Android", "Mobile & Tablet")
                        PlatformPill("iOS", "iPhone & iPad")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlatformPill("Desktop", "macOS, Windows, Linux")
                        DividerDot()
                        PlatformPill("Android", "Mobile & Tablet")
                        DividerDot()
                        PlatformPill("iOS", "iPhone & iPad")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformPill(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = subtitle, color = Color(0xFF899390), fontSize = 12.sp)
    }
}

@Composable
private fun DividerDot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(Color(0xFF3F4946))
    )
}
