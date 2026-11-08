package com.serratocreations.phovo.website.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLAnchorElement

/** An inline SVG from [IconSvg]. It inherits the text color and scales with the font size. */
@Composable
fun Icon(svg: String, className: String = "icon") {
    Span({
        classes(className)
        attr("aria-hidden", "true")
        ref { element ->
            element.innerHTML = svg
            onDispose { }
        }
    })
}

@Composable
fun Brand() {
    A("/", {
        classes("brand")
        attr("aria-label", "Phovo home")
    }) {
        Img("/media/icon-128.webp", alt = "") {
            attr("width", "32")
            attr("height", "32")
        }
        Icon(IconSvg.wordmark, "wordmark")
    }
}

@Composable
fun ExternalLink(
    href: String,
    classes: List<String> = emptyList(),
    content: ContentBuilder<HTMLAnchorElement>,
) {
    A(href, {
        classes(classes)
        target(ATarget.Blank)
        attr("rel", "noopener")
    }, content)
}

@Composable
fun SectionHead(eyebrow: String, title: String, body: String? = null, centered: Boolean = false) {
    Div({ classes(*listOfNotNull("section-head", "center".takeIf { centered }).toTypedArray()) }) {
        Span({ classes("eyebrow") }) { Text(eyebrow) }
        H2 { Text(title) }
        if (body != null) P { Text(body) }
    }
}

/** A screenshot with its real size declared, so the layout doesn't jump while it loads. */
@Composable
fun Shot(src: String, alt: String, width: Int, height: Int, className: String = "shot", eager: Boolean = false) {
    Img(src, alt) {
        classes(className)
        attr("width", width.toString())
        attr("height", height.toString())
        attr("decoding", "async")
        if (!eager) attr("loading", "lazy")
    }
}
