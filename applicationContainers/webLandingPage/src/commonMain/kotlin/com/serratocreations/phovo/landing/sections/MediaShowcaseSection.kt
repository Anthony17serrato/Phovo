package com.serratocreations.phovo.landing.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import phovo.applicationcontainers.weblandingpage.generated.resources.Res
import phovo.applicationcontainers.weblandingpage.generated.resources.desktop_screenshot
import phovo.applicationcontainers.weblandingpage.generated.resources.iphone_screenshot

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
                    text = "Preview Phovo running natively on iOS mobile devices and Desktop workstations.",
                    color = Color(0xFFBEC9C5),
                    fontSize = if (isMobile) 13.sp else 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isMobile) 18.sp else 22.sp
                )
            }

            // Dual showcase container (Responsive)
            if (isMobile) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IosShowcaseCard(isMobile = true)
                    DesktopShowcaseCard(isMobile = true)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        IosShowcaseCard(isMobile = false)
                    }
                    Box(modifier = Modifier.weight(1.3f)) {
                        DesktopShowcaseCard(isMobile = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun IosShowcaseCard(isMobile: Boolean) {
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
                    text = "iOS Mobile Interface",
                    color = Color(0xFF9FF2E2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Perfectly Centered Uncropped iPhone Screenshot Box
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(if (isMobile) 0.85f else 0.9f)
                    .aspectRatio(0.48f)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF090F0E))
                    .border(2.dp, Color(0xFF3F4946), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.iphone_screenshot),
                    contentDescription = "iPhone Screenshot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "iOS 16+ iPhone 16 Optimized",
                color = Color(0xFF899390),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DesktopShowcaseCard(isMobile: Boolean) {
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
                    .aspectRatio(1.5f)
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
