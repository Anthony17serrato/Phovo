package com.serratocreations.phovo.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.serratocreations.phovo.landing.components.Header
import com.serratocreations.phovo.landing.sections.*
import kotlinx.coroutines.launch

@Composable
fun LandingPageApp() {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0E1513),
            surface = Color(0xFF141D1A),
            primary = Color(0xFF006B5F),
            secondary = Color(0xFF83D5C6),
            onBackground = Color.White,
            onSurface = Color(0xFFBEC9C5)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E1513))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header Bar
                Header(
                    onScrollToSection = { sectionKey ->
                        coroutineScope.launch {
                            val max = scrollState.maxValue.toFloat()
                            if (max > 0f) {
                                when (sectionKey) {
                                    "hero" -> scrollState.animateScrollTo(0)
                                    "features" -> scrollState.animateScrollTo((max * 0.35f).toInt())
                                    "platforms" -> scrollState.animateScrollTo((max * 0.20f).toInt())
                                    "media" -> scrollState.animateScrollTo((max * 0.65f).toInt())
                                    "roadmap" -> scrollState.animateScrollTo((max * 0.85f).toInt())
                                }
                            }
                        }
                    }
                )

                // Hero Section
                HeroSection(
                    onExploreClick = {
                        coroutineScope.launch {
                            val max = scrollState.maxValue.toFloat()
                            if (max > 0f) {
                                scrollState.animateScrollTo((max * 0.35f).toInt())
                            }
                        }
                    }
                )

                // Platform Grid Section (Desktop, Android, iOS)
                PlatformGridSection()

                // Feature Highlights
                FeatureCardsSection()

                // Media Showcase
                MediaShowcaseSection()

                // Development Timeline
                TimelineSection()

                // Footer
                FooterSection()
            }
        }
    }
}
