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

data class Milestone(
    val badge: String,
    val title: String,
    val date: String,
    val availability: String,
    val focusPoints: List<String>,
    val isActive: Boolean = false
)

@Composable
fun TimelineSection() {
    val milestones = listOf(
        Milestone(
            badge = "CURRENT MILESTONE",
            title = "Alpha Release",
            date = "September 30, 2026",
            availability = "Internal & Early Access",
            focusPoints = listOf(
                "Early testing of core backup engine",
                "Rapid feature iteration",
                "Initial KMP web & mobile client builds"
            ),
            isActive = true
        ),
        Milestone(
            badge = "UPCOMING",
            title = "Beta Release",
            date = "February 28, 2027",
            availability = "Public Beta",
            focusPoints = listOf(
                "Initial feature set development complete",
                "No further breaking architecture changes",
                "Begin platform stabilization & bug fixes"
            )
        ),
        Milestone(
            badge = "GO-LIVE",
            title = "Stable Release",
            date = "May 31, 2027",
            availability = "Android, iOS & Desktop",
            focusPoints = listOf(
                "Production-ready cross-platform release",
                "All critical & high-priority issues resolved",
                "Full multi-device synchronization engine"
            )
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
                    text = "RELEASE SCHEDULE",
                    color = Color(0xFF83D5C6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Phovo Development Roadmap",
                    color = Color.White,
                    fontSize = if (isMobile) 24.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isMobile) 32.sp else 40.sp
                )

                Text(
                    text = "Phovo is undergoing active development. Early Alpha builds launch September 30, 2026.",
                    color = Color(0xFFBEC9C5),
                    fontSize = if (isMobile) 13.sp else 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isMobile) 18.sp else 22.sp
                )
            }

            // Timeline Milestone Cards (Responsive)
            if (isMobile) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    milestones.forEach { milestone ->
                        MilestoneCard(milestone)
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    milestones.forEach { milestone ->
                        Box(modifier = Modifier.weight(1f)) {
                            MilestoneCard(milestone)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MilestoneCard(
    milestone: Milestone
) {
    val borderColor = if (milestone.isActive) Color(0xFF83D5C6) else Color(0x3383D5C6)
    val bgColor = if (milestone.isActive) Color(0xFF1B2925) else Color(0xFF141D1A)

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (milestone.isActive) Color(0xFF006B5F) else Color(0x2283D5C6),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = milestone.badge,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Text(
                text = milestone.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = milestone.date,
                color = Color(0xFF9FF2E2),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Availability: ${milestone.availability}",
                color = Color(0xFF899390),
                fontSize = 12.sp
            )

            HorizontalDivider(color = Color(0x223F4946))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                milestone.focusPoints.forEach { point ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", color = Color(0xFF83D5C6), fontSize = 12.sp)
                        Text(point, color = Color(0xFFBEC9C5), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}
