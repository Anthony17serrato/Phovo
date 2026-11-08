package com.serratocreations.phovo.website

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.App
import org.jetbrains.compose.web.css.Style

@App
@Composable
fun AppEntry(content: @Composable () -> Unit) {
    Style(SiteStyles)
    content()
}
