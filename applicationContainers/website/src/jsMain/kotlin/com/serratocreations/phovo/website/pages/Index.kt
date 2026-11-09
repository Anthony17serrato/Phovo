package com.serratocreations.phovo.website.pages

import androidx.compose.runtime.Composable
import com.serratocreations.phovo.website.components.BuiltInTheOpen
import com.serratocreations.phovo.website.components.Closer
import com.serratocreations.phovo.website.components.DownloadSection
import com.serratocreations.phovo.website.components.Faq
import com.serratocreations.phovo.website.components.Features
import com.serratocreations.phovo.website.components.Gallery
import com.serratocreations.phovo.website.components.Hero
import com.serratocreations.phovo.website.components.HowItWorks
import com.serratocreations.phovo.website.components.Roadmap
import com.serratocreations.phovo.website.components.SiteFooter
import com.serratocreations.phovo.website.components.SiteNav
import com.serratocreations.phovo.website.components.Why
import com.serratocreations.phovo.website.components.rememberDownloadState
import com.varabyte.kobweb.core.Page
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Text

@Page
@Composable
fun HomePage() {
    val downloads = rememberDownloadState()
    A("#main", { classes("skip") }) { Text("Skip to content") }
    SiteNav()
    Main({ id("main") }) {
        Hero(downloads)
        HowItWorks()
        Features()
        Why()
        Gallery()
        DownloadSection(downloads)
        Roadmap()
        BuiltInTheOpen()
        Faq()
        Closer()
    }
    SiteFooter()
}
