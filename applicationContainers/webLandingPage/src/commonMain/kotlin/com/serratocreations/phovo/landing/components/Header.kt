package com.serratocreations.phovo.landing.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import phovo.applicationcontainers.weblandingpage.generated.resources.Res
import phovo.applicationcontainers.weblandingpage.generated.resources.phovo_icon

@Composable
fun Header(
    onScrollToSection: (String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xEE090F0E))
            .border(1.dp, Color(0x2283D5C6))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val isMobile = maxWidth < 768.dp

        if (isMobile) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Logo & Brand Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onScrollToSection("hero") }
                ) {
                    Image(
                        painter = painterResource(Res.drawable.phovo_icon),
                        contentDescription = "Phovo Logo",
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = "phovo",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        letterSpacing = (-0.5).sp
                    )

                    Surface(
                        color = Color(0x3383D5C6),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Alpha Aug 31",
                            color = Color(0xFF9FF2E2),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Bottom Row: Horizontal Scrollable Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Overview",
                        color = Color(0xFFBEC9C5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clickable { onScrollToSection("features") }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Text(
                        text = "Platforms",
                        color = Color(0xFFBEC9C5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clickable { onScrollToSection("platforms") }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Text(
                        text = "Screenshots",
                        color = Color(0xFFBEC9C5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clickable { onScrollToSection("media") }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Text(
                        text = "Roadmap",
                        color = Color(0xFFBEC9C5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clickable { onScrollToSection("roadmap") }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.clickable { onScrollToSection("hero") }
                ) {
                    Image(
                        painter = painterResource(Res.drawable.phovo_icon),
                        contentDescription = "Phovo Logo",
                        modifier = Modifier.size(36.dp)
                    )

                    Text(
                        text = "phovo",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp
                    )

                    Surface(
                        color = Color(0x3383D5C6),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "Alpha Aug 31",
                            color = Color(0xFF9FF2E2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Navigation Links
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Overview",
                        color = Color(0xFFBEC9C5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.clickable { onScrollToSection("features") }
                    )

                    Text(
                        text = "Platforms",
                        color = Color(0xFFBEC9C5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.clickable { onScrollToSection("platforms") }
                    )

                    Text(
                        text = "Screenshots",
                        color = Color(0xFFBEC9C5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.clickable { onScrollToSection("media") }
                    )

                    Text(
                        text = "Roadmap",
                        color = Color(0xFFBEC9C5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.clickable { onScrollToSection("roadmap") }
                    )
                }
            }
        }
    }
}
