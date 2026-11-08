package com.serratocreations.phovo.website

import org.jetbrains.compose.web.css.CSSMediaQuery
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.StylePropertyValue
import org.jetbrains.compose.web.css.media
import org.jetbrains.compose.web.css.percent

private fun StyleScope.css(vararg declarations: Pair<String, String>) {
    declarations.forEach { (name, value) -> property(name, value) }
}

private fun feature(name: String, value: String) =
    CSSMediaQuery.MediaFeature(name, StylePropertyValue(value))

private val light = feature("prefers-color-scheme", "light")
private val reducedMotion = feature("prefers-reduced-motion", "reduce")
private val phone = feature("max-width", "720px")
private val narrow = feature("max-width", "960px")

/**
 * The whole site's CSS. Colors come from the app's Material 3 scheme in core/designsystem
 * (primary #006B5F, primary container #9FF2E2, dark surface #0E1513); the four glow colors are
 * the petals of the app icon. Display type is Fraunces at the wordmark's axes (docs/branding).
 */
object SiteStyles : StyleSheet() {
    // Declared before init, which reads its generated name.
    val travel by keyframes {
        from { css("left" to "0%", "opacity" to "0") }
        each(15.percent) { css("opacity" to "1") }
        each(85.percent) { css("opacity" to "1") }
        to { css("left" to "100%", "opacity" to "0") }
    }

    init {
        // ---------- Tokens ----------
        ":root" style {
            css(
                "--bg" to "#0A100F",
                "--bg-raised" to "#0F1715",
                "--card" to "#121C1A",
                "--card-hover" to "#16221F",
                "--line" to "rgba(159, 242, 226, 0.10)",
                "--line-strong" to "rgba(159, 242, 226, 0.22)",
                "--text" to "#E9F2EF",
                "--muted" to "#9CB0AA",
                "--faint" to "#6E837D",
                "--accent" to "#83D5C6",
                "--accent-text" to "#9FF2E2",
                "--btn" to "#9FF2E2",
                "--btn-hover" to "#B8F8EB",
                "--btn-text" to "#00201C",
                "--chip" to "rgba(131, 213, 198, 0.12)",
                "--shadow" to "0 30px 80px -30px rgba(0, 0, 0, 0.75)",
                "--glow-opacity" to "0.28",
                "--display" to "'Fraunces Display', 'Iowan Old Style', Georgia, serif",
                "--sans" to "ui-sans-serif, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
                "--mono" to "ui-monospace, 'SF Mono', Menlo, Consolas, monospace",
                "--radius" to "20px",
                "--max" to "1180px",
                "color-scheme" to "dark",
            )
        }
        media(light) {
            ":root" style {
                css(
                    "--bg" to "#F5FAF8",
                    "--bg-raised" to "#EDF5F2",
                    "--card" to "#FFFFFF",
                    "--card-hover" to "#FBFEFD",
                    "--line" to "rgba(0, 80, 71, 0.10)",
                    "--line-strong" to "rgba(0, 80, 71, 0.20)",
                    "--text" to "#0E1513",
                    "--muted" to "#4A5C57",
                    "--faint" to "#71837E",
                    "--accent" to "#006B5F",
                    "--accent-text" to "#005047",
                    "--btn" to "#006B5F",
                    "--btn-hover" to "#00594F",
                    "--btn-text" to "#FFFFFF",
                    "--chip" to "rgba(0, 107, 95, 0.08)",
                    "--shadow" to "0 30px 70px -32px rgba(14, 40, 35, 0.35)",
                    "--glow-opacity" to "0.20",
                    "color-scheme" to "light",
                )
            }
        }

        "@font-face" style {
            css(
                "font-family" to "'Fraunces Display'",
                "src" to "url('/fonts/fraunces-display.woff2') format('woff2')",
                "font-weight" to "400 700",
                "font-display" to "swap",
            )
        }

        // ---------- Base ----------
        "*, *::before, *::after" style { css("box-sizing" to "border-box") }
        "html" style {
            css("scroll-behavior" to "smooth", "scroll-padding-top" to "88px", "-webkit-text-size-adjust" to "100%")
        }
        "body" style {
            css(
                "margin" to "0",
                "background" to "var(--bg)",
                "color" to "var(--text)",
                "font-family" to "var(--sans)",
                "font-size" to "17px",
                "line-height" to "1.6",
                "-webkit-font-smoothing" to "antialiased",
                "overflow-x" to "hidden",
            )
        }
        "img, video, svg" style { css("display" to "block", "max-width" to "100%") }
        "a" style { css("color" to "inherit", "text-decoration" to "none") }
        "p" style { css("margin" to "0") }
        "h1, h2, h3" style {
            css("margin" to "0", "font-family" to "var(--display)", "font-weight" to "600", "letter-spacing" to "-0.015em", "line-height" to "1.08", "text-wrap" to "balance")
        }
        "code" style {
            css("font-family" to "var(--mono)", "font-size" to "0.88em", "padding" to "0.1em 0.4em", "border-radius" to "6px", "background" to "var(--chip)")
        }
        ":focus-visible" style { css("outline" to "2px solid var(--accent)", "outline-offset" to "3px", "border-radius" to "8px") }
        "::selection" style { css("background" to "rgba(131, 213, 198, 0.35)") }

        ".icon" style { css("display" to "inline-flex", "width" to "1.15em", "height" to "1.15em", "flex" to "none") }
        ".icon svg" style { css("width" to "100%", "height" to "100%") }
        ".container" style { css("width" to "100%", "max-width" to "var(--max)", "margin" to "0 auto", "padding" to "0 24px") }
        ".skip" style {
            css("position" to "absolute", "left" to "-999px", "top" to "8px", "z-index" to "100", "padding" to "8px 14px", "background" to "var(--btn)", "color" to "var(--btn-text)", "border-radius" to "10px")
        }
        ".skip:focus" style { css("left" to "16px") }

        // ---------- Buttons ----------
        ".btn" style {
            css(
                "display" to "inline-flex", "align-items" to "center", "gap" to "10px",
                "padding" to "14px 22px", "border-radius" to "999px", "border" to "1px solid transparent",
                "font" to "600 16px/1.2 var(--sans)", "cursor" to "pointer", "white-space" to "nowrap",
                "transition" to "background 160ms ease, transform 160ms ease, border-color 160ms ease",
            )
        }
        ".btn:active" style { css("transform" to "translateY(1px)") }
        ".btn-primary" style { css("background" to "var(--btn)", "color" to "var(--btn-text)", "box-shadow" to "0 10px 30px -12px rgba(131, 213, 198, 0.6)") }
        ".btn-primary:hover" style { css("background" to "var(--btn-hover)") }
        ".btn-ghost" style { css("color" to "var(--text)", "border-color" to "var(--line-strong)", "background" to "transparent") }
        ".btn-ghost:hover" style { css("background" to "var(--chip)") }
        ".btn-small" style { css("padding" to "9px 16px", "font-size" to "14px") }
        ".btn-big" style { css("padding" to "16px 26px 16px 22px", "font-size" to "17px") }
        ".btn-big .icon" style { css("width" to "1.3em", "height" to "1.3em") }
        ".link" style { css("color" to "var(--accent-text)", "font-weight" to "600", "display" to "inline-flex", "align-items" to "center", "gap" to "6px") }
        ".link:hover" style { css("text-decoration" to "underline", "text-underline-offset" to "3px") }
        "button.link" style { css("background" to "none", "border" to "0", "padding" to "0", "font" to "inherit", "cursor" to "pointer") }

        // ---------- Nav ----------
        ".nav" style {
            css(
                "position" to "sticky", "top" to "0", "z-index" to "50",
                "background" to "color-mix(in srgb, var(--bg) 78%, transparent)",
                "backdrop-filter" to "saturate(160%) blur(16px)", "-webkit-backdrop-filter" to "saturate(160%) blur(16px)",
                "border-bottom" to "1px solid var(--line)",
            )
        }
        ".nav-inner" style { css("display" to "flex", "align-items" to "center", "gap" to "28px", "height" to "68px") }
        ".brand" style { css("display" to "inline-flex", "align-items" to "center", "gap" to "10px", "color" to "var(--accent)") }
        ".brand img" style { css("width" to "32px", "height" to "32px") }
        ".brand .wordmark" style { css("width" to "92px", "height" to "28px") }
        ".brand .wordmark svg" style { css("width" to "100%", "height" to "100%") }
        ".nav-links" style { css("display" to "flex", "gap" to "24px", "margin-left" to "12px", "font-size" to "15px", "color" to "var(--muted)") }
        ".nav-links a:hover" style { css("color" to "var(--text)") }
        ".nav-actions" style { css("margin-left" to "auto", "display" to "flex", "align-items" to "center", "gap" to "10px") }
        media(narrow) { ".nav-links" style { css("display" to "none") } }
        media(phone) { ".nav-github-label" style { css("display" to "none") } }

        // ---------- Hero ----------
        ".hero" style { css("position" to "relative", "padding" to "72px 0 96px", "overflow" to "hidden") }
        ".hero-grid" style {
            css("display" to "grid", "grid-template-columns" to "minmax(0, 1.12fr) minmax(0, 1fr)", "gap" to "56px", "align-items" to "center")
        }
        ".glow" style {
            css("position" to "absolute", "inset" to "-10% -10% auto auto", "width" to "900px", "height" to "900px", "pointer-events" to "none", "opacity" to "var(--glow-opacity)", "filter" to "blur(90px)", "z-index" to "0")
        }
        ".glow span" style { css("position" to "absolute", "width" to "46%", "height" to "46%", "border-radius" to "50%") }
        ".glow span:nth-child(1)" style { css("left" to "4%", "top" to "4%", "background" to "#F2545B") }
        ".glow span:nth-child(2)" style { css("right" to "4%", "top" to "4%", "background" to "#3D7BFA") }
        ".glow span:nth-child(3)" style { css("left" to "4%", "bottom" to "4%", "background" to "#6CC24A") }
        ".glow span:nth-child(4)" style { css("right" to "4%", "bottom" to "4%", "background" to "#1FB5A6") }
        ".hero-copy" style { css("position" to "relative", "z-index" to "1") }
        ".pill" style {
            css(
                "display" to "inline-flex", "align-items" to "center", "gap" to "10px",
                "padding" to "6px 14px 6px 6px", "border-radius" to "999px",
                "border" to "1px solid var(--line-strong)", "background" to "var(--bg-raised)",
                "font-size" to "14px", "color" to "var(--muted)",
            )
        }
        ".pill b" style {
            css("padding" to "3px 10px", "border-radius" to "999px", "background" to "var(--btn)", "color" to "var(--btn-text)", "font-size" to "12px", "letter-spacing" to "0.02em")
        }
        ".pill:hover" style { css("border-color" to "var(--accent)", "color" to "var(--text)") }
        ".hero h1" style { css("margin-top" to "24px", "font-size" to "clamp(44px, 5.6vw, 74px)", "line-height" to "1.0", "letter-spacing" to "-0.025em") }
        ".hero h1 em" style { css("font-style" to "normal", "color" to "var(--accent)") }
        ".lede" style { css("margin-top" to "22px", "max-width" to "540px", "font-size" to "19px", "color" to "var(--muted)") }
        ".cta" style { css("margin-top" to "34px", "display" to "flex", "flex-direction" to "column", "align-items" to "flex-start", "gap" to "14px") }
        ".cta-row" style { css("display" to "flex", "flex-wrap" to "wrap", "align-items" to "center", "gap" to "12px") }
        ".cta-meta" style { css("font-size" to "14px", "color" to "var(--faint)", "display" to "flex", "flex-wrap" to "wrap", "gap" to "6px 14px") }
        ".cta-meta a" style { css("color" to "var(--muted)", "text-decoration" to "underline", "text-underline-offset" to "3px", "text-decoration-color" to "var(--line-strong)") }
        ".cta-meta a:hover" style { css("color" to "var(--text)") }
        ".trust" style { css("margin-top" to "36px", "display" to "flex", "flex-wrap" to "wrap", "gap" to "10px 22px", "font-size" to "14px", "color" to "var(--muted)") }
        ".trust span" style { css("display" to "inline-flex", "align-items" to "center", "gap" to "8px") }
        ".trust .icon" style { css("color" to "var(--accent)") }

        ".notice" style {
            css(
                "max-width" to "520px", "padding" to "18px 20px", "border-radius" to "16px",
                "border" to "1px solid var(--line-strong)", "background" to "var(--card)",
                "display" to "grid", "gap" to "10px", "font-size" to "15px", "color" to "var(--muted)",
            )
        }
        ".notice strong" style { css("color" to "var(--text)", "font-size" to "17px") }
        ".notice ol" style { css("margin" to "0", "padding-left" to "20px", "display" to "grid", "gap" to "6px") }
        ".hint" style { css("font-size" to "14px", "color" to "var(--muted)", "max-width" to "480px") }

        // Device composition: the desktop timeline, with the phone walkthrough in front of it.
        // The composition is a size container, so the phone's bezel and corners scale with it (cqi = 1% of
        // its width). The phone is 34cqi wide; corners at ~16% of that match a real iPhone at any size.
        ".devices" style { css("position" to "relative", "z-index" to "1", "aspect-ratio" to "1 / 0.92", "container-type" to "inline-size") }
        ".window" style {
            css(
                "position" to "absolute", "right" to "0", "top" to "4%", "width" to "92%",
                "border-radius" to "14px", "overflow" to "hidden", "background" to "#000",
                "border" to "1px solid rgba(255, 255, 255, 0.08)", "box-shadow" to "var(--shadow)",
            )
        }
        ".window img" style { css("width" to "100%", "height" to "auto") }
        ".phone" style {
            css(
                "position" to "absolute", "left" to "0", "bottom" to "0", "width" to "34%",
                "padding" to "1.2cqi", "border-radius" to "5.6cqi", "background" to "#0B0D0D",
                "border" to "1px solid rgba(255, 255, 255, 0.14)",
                "box-shadow" to "0 40px 80px -24px rgba(0, 0, 0, 0.7), inset 0 0 0 1px rgba(0, 0, 0, 0.6)",
            )
        }
        ".devices-wrap" style { css("position" to "relative", "z-index" to "1") }
        ".devices-caption" style { css("margin-top" to "16px", "font-size" to "13px", "color" to "var(--faint)") }
        ".phone video, .phone img" style { css("width" to "100%", "height" to "auto", "border-radius" to "4.4cqi", "background" to "#F2F2F2") }
        media(narrow) {
            ".hero" style { css("padding" to "48px 0 72px") }
            ".hero-grid" style { css("grid-template-columns" to "1fr", "gap" to "48px") }
            ".devices-wrap" style { css("max-width" to "640px", "width" to "100%", "margin" to "0 auto") }
            ".glow" style { css("inset" to "auto -40% -10% auto", "width" to "700px", "height" to "700px") }
        }
        media(phone) {
            ".hero h1" style { css("font-size" to "44px") }
            ".lede" style { css("font-size" to "17px") }
            ".btn-big" style { css("width" to "100%", "justify-content" to "center") }
            ".cta, .cta-row" style { css("width" to "100%") }
        }

        // ---------- Sections ----------
        ".section" style { css("padding" to "112px 0", "position" to "relative") }
        ".section-alt" style { css("background" to "var(--bg-raised)", "border-top" to "1px solid var(--line)", "border-bottom" to "1px solid var(--line)") }
        ".eyebrow" style {
            css("font-size" to "13px", "font-weight" to "700", "letter-spacing" to "0.14em", "text-transform" to "uppercase", "color" to "var(--accent)")
        }
        ".section-head" style { css("max-width" to "720px", "margin-bottom" to "56px", "display" to "grid", "gap" to "16px") }
        ".section-head.center" style { css("margin-left" to "auto", "margin-right" to "auto", "text-align" to "center", "justify-items" to "center") }
        ".section-head h2" style { css("font-size" to "clamp(34px, 4.4vw, 54px)") }
        ".section-head p" style { css("font-size" to "18px", "color" to "var(--muted)") }
        media(phone) { ".section" style { css("padding" to "80px 0") } }

        // How it works
        ".steps" style { css("display" to "grid", "grid-template-columns" to "repeat(3, minmax(0, 1fr))", "gap" to "20px", "counter-reset" to "step") }
        ".step" style {
            css("position" to "relative", "padding" to "28px", "border-radius" to "var(--radius)", "background" to "var(--card)", "border" to "1px solid var(--line)", "display" to "grid", "gap" to "10px", "align-content" to "start")
        }
        ".step-num" style {
            css("width" to "40px", "height" to "40px", "border-radius" to "12px", "display" to "grid", "place-items" to "center", "background" to "var(--chip)", "color" to "var(--accent)", "font" to "600 20px/1 var(--display)", "margin-bottom" to "8px")
        }
        ".step h3" style { css("font-size" to "22px") }
        ".step p" style { css("color" to "var(--muted)", "font-size" to "16px") }
        media(narrow) { ".steps" style { css("grid-template-columns" to "1fr") } }

        // The phone → Wi-Fi → computer strip above the steps
        ".flow" style { css("display" to "flex", "align-items" to "center", "justify-content" to "center", "gap" to "0", "margin" to "0 auto 48px", "max-width" to "560px", "color" to "var(--accent)") }
        ".flow-node" style {
            css("width" to "64px", "height" to "64px", "border-radius" to "18px", "display" to "grid", "place-items" to "center", "background" to "var(--card)", "border" to "1px solid var(--line-strong)", "flex" to "none")
        }
        ".flow-node .icon" style { css("width" to "28px", "height" to "28px") }
        ".flow-line" style { css("position" to "relative", "flex" to "1", "height" to "2px", "margin" to "0 12px", "background" to "repeating-linear-gradient(90deg, var(--line-strong) 0 6px, transparent 6px 12px)", "overflow" to "visible") }
        ".flow-line i" style {
            css("position" to "absolute", "top" to "-3px", "left" to "0", "width" to "8px", "height" to "8px", "border-radius" to "50%", "background" to "var(--accent)", "box-shadow" to "0 0 12px var(--accent)", "animation" to "${travel.name} 2.6s cubic-bezier(.5,0,.5,1) infinite")
        }
        ".flow-line i:nth-child(2)" style { css("animation-delay" to "0.85s") }
        ".flow-line i:nth-child(3)" style { css("animation-delay" to "1.7s") }
        ".flow-wifi" style { css("flex" to "none", "color" to "var(--muted)") }
        ".flow-wifi .icon" style { css("width" to "22px", "height" to "22px") }
        media(reducedMotion) { ".flow-line i" style { css("animation" to "none", "opacity" to "0") } }

        // Showcase rows
        ".showcase" style { css("display" to "grid", "gap" to "120px") }
        ".show" style { css("display" to "grid", "grid-template-columns" to "minmax(0, 1fr) minmax(0, 1fr)", "gap" to "72px", "align-items" to "center") }
        ".show.flip .show-media" style { css("order" to "2") }
        ".show-copy" style { css("display" to "grid", "gap" to "16px", "max-width" to "480px") }
        ".show-copy h3" style { css("font-size" to "clamp(30px, 3.4vw, 42px)") }
        ".show-copy p" style { css("color" to "var(--muted)", "font-size" to "18px") }
        ".show-media" style {
            css("position" to "relative", "border-radius" to "28px", "padding" to "48px 0 0", "overflow" to "hidden", "display" to "flex", "justify-content" to "center", "background" to "radial-gradient(120% 90% at 50% 0%, var(--chip), transparent 70%), var(--card)", "border" to "1px solid var(--line)", "min-height" to "460px")
        }
        ".show-media .shot" style { css("width" to "min(300px, 72%)", "height" to "auto", "border-radius" to "28px 28px 0 0", "box-shadow" to "var(--shadow)", "border" to "1px solid var(--line)", "border-bottom" to "0", "align-self" to "flex-end", "max-height" to "520px", "object-fit" to "cover", "object-position" to "top") }
        // The desktop window floats inside its card with even padding, like the one in the hero.
        ".show-media.wide" style { css("padding" to "clamp(20px, 4vw, 40px)", "justify-content" to "center", "align-items" to "center", "min-height" to "0") }
        ".show-media.wide .shot" style {
            css("width" to "100%", "max-height" to "none", "align-self" to "center", "border-radius" to "12px", "border" to "1px solid rgba(255, 255, 255, 0.08)", "box-shadow" to "var(--shadow)")
        }
        ".points" style { css("list-style" to "none", "margin" to "8px 0 0", "padding" to "0", "display" to "grid", "gap" to "10px", "color" to "var(--muted)", "font-size" to "16px") }
        ".points li" style { css("display" to "flex", "gap" to "10px", "align-items" to "flex-start") }
        ".points .icon" style { css("color" to "var(--accent)", "margin-top" to "4px") }
        media(narrow) {
            ".show" style { css("grid-template-columns" to "1fr", "gap" to "36px") }
            ".show.flip .show-media" style { css("order" to "0") }
            ".showcase" style { css("gap" to "88px") }
            ".show-media" style { css("min-height" to "0") }
        }

        // Why grid
        ".why" style { css("display" to "grid", "grid-template-columns" to "repeat(3, minmax(0, 1fr))", "gap" to "20px") }
        ".why-card" style {
            css("padding" to "30px", "border-radius" to "var(--radius)", "background" to "var(--card)", "border" to "1px solid var(--line)", "display" to "grid", "gap" to "12px", "align-content" to "start", "transition" to "border-color 160ms ease, background 160ms ease")
        }
        ".why-card:hover" style { css("border-color" to "var(--line-strong)", "background" to "var(--card-hover)") }
        ".why-icon" style { css("width" to "44px", "height" to "44px", "border-radius" to "13px", "display" to "grid", "place-items" to "center", "background" to "var(--chip)", "color" to "var(--accent)", "margin-bottom" to "6px") }
        ".why-icon .icon" style { css("width" to "22px", "height" to "22px") }
        ".why-card h3" style { css("font-size" to "22px") }
        ".why-card p" style { css("color" to "var(--muted)", "font-size" to "16px") }
        media(narrow) { ".why" style { css("grid-template-columns" to "1fr") } }

        // Gallery strip
        ".gallery" style {
            css("display" to "flex", "justify-content" to "safe center", "gap" to "20px", "overflow-x" to "auto", "scroll-snap-type" to "x mandatory", "padding" to "8px 24px 28px", "margin" to "0 auto", "max-width" to "calc(var(--max) + 48px)", "scrollbar-width" to "thin")
        }
        ".gallery figure" style { css("margin" to "0", "flex" to "0 0 auto", "width" to "216px", "scroll-snap-align" to "center", "display" to "grid", "gap" to "12px") }
        ".gallery img" style { css("width" to "100%", "height" to "auto", "border-radius" to "26px", "border" to "1px solid var(--line)", "box-shadow" to "var(--shadow)") }
        ".gallery figcaption" style { css("font-size" to "14px", "color" to "var(--muted)", "text-align" to "center") }

        // Platforms
        ".platforms" style { css("display" to "grid", "grid-template-columns" to "repeat(6, minmax(0, 1fr))", "gap" to "12px") }
        ".platform" style {
            css("padding" to "22px 16px", "border-radius" to "18px", "background" to "var(--card)", "border" to "1px solid var(--line)", "display" to "grid", "justify-items" to "center", "gap" to "10px", "text-align" to "center", "font-size" to "15px", "font-weight" to "600")
        }
        ".platform .icon" style { css("width" to "30px", "height" to "30px", "color" to "var(--text)") }
        ".platform small" style { css("font-weight" to "400", "color" to "var(--faint)", "font-size" to "13px", "line-height" to "1.35") }
        ".platform.soon" style { css("opacity" to "0.6", "border-style" to "dashed") }
        media(narrow) { ".platforms" style { css("grid-template-columns" to "repeat(3, minmax(0, 1fr))") } }
        media(phone) { ".platforms" style { css("grid-template-columns" to "repeat(2, minmax(0, 1fr))") } }

        // ---------- Downloads ----------
        ".dl-grid" style { css("display" to "grid", "grid-template-columns" to "repeat(3, minmax(0, 1fr))", "gap" to "20px") }
        ".dl-card" style {
            css("padding" to "26px", "border-radius" to "var(--radius)", "background" to "var(--card)", "border" to "1px solid var(--line)", "display" to "grid", "gap" to "16px", "align-content" to "start")
        }
        ".dl-card.mine" style { css("border-color" to "var(--accent)", "box-shadow" to "0 0 0 1px var(--accent), 0 24px 60px -30px rgba(131, 213, 198, 0.5)") }
        ".dl-head" style { css("display" to "flex", "align-items" to "center", "gap" to "12px") }
        ".dl-head .icon" style { css("width" to "28px", "height" to "28px") }
        ".dl-head h3" style { css("font-size" to "24px") }
        ".badge" style { css("margin-left" to "auto", "font-size" to "12px", "font-weight" to "700", "padding" to "4px 10px", "border-radius" to "999px", "background" to "var(--btn)", "color" to "var(--btn-text)", "white-space" to "nowrap") }
        ".dl-list" style { css("display" to "grid", "gap" to "8px") }
        ".dl-item" style {
            css("display" to "grid", "grid-template-columns" to "1fr auto", "align-items" to "center", "gap" to "4px 12px", "padding" to "12px 14px", "border-radius" to "14px", "border" to "1px solid var(--line)", "transition" to "border-color 140ms ease, background 140ms ease")
        }
        "a.dl-item:hover" style { css("border-color" to "var(--accent)", "background" to "var(--chip)") }
        ".dl-item b" style { css("font-size" to "15px") }
        ".dl-item span" style { css("grid-column" to "1", "font-size" to "13px", "color" to "var(--faint)", "line-height" to "1.4") }
        ".dl-item .icon" style { css("grid-row" to "1 / span 2", "grid-column" to "2", "color" to "var(--accent)") }
        ".dl-item em" style { css("font-style" to "normal", "color" to "var(--muted)") }
        ".dl-card p" style { css("font-size" to "15px", "color" to "var(--muted)") }
        ".dl-foot" style { css("margin-top" to "28px", "display" to "flex", "flex-wrap" to "wrap", "gap" to "10px 28px", "font-size" to "15px", "color" to "var(--muted)") }
        ".dl-note" style { css("margin-top" to "44px", "display" to "grid", "align-items" to "start", "grid-template-columns" to "repeat(2, minmax(0, 1fr))", "gap" to "20px") }
        media(narrow) {
            ".dl-grid" style { css("grid-template-columns" to "repeat(2, minmax(0, 1fr))") }
            ".dl-note" style { css("grid-template-columns" to "1fr") }
        }
        media(phone) { ".dl-grid" style { css("grid-template-columns" to "1fr") } }

        // details/summary, shared by the first-launch notes and the FAQ
        ".fold" style { css("border-radius" to "16px", "background" to "var(--card)", "border" to "1px solid var(--line)") }
        ".fold summary" style {
            css("list-style" to "none", "cursor" to "pointer", "padding" to "18px 22px", "display" to "flex", "align-items" to "center", "gap" to "12px", "font-weight" to "600")
        }
        ".fold summary::-webkit-details-marker" style { css("display" to "none") }
        ".fold summary .icon:last-child" style { css("margin-left" to "auto", "color" to "var(--muted)", "transition" to "transform 200ms ease") }
        ".fold[open] summary .icon:last-child" style { css("transform" to "rotate(180deg)") }
        ".fold-body" style { css("padding" to "0 22px 20px", "color" to "var(--muted)", "font-size" to "16px", "display" to "grid", "gap" to "10px") }
        ".fold-body ol" style { css("margin" to "0", "padding-left" to "20px", "display" to "grid", "gap" to "6px") }
        ".fold-body strong" style { css("color" to "var(--text)") }
        ".faq" style { css("display" to "grid", "gap" to "12px", "max-width" to "820px", "margin" to "0 auto") }
        ".faq .fold summary" style { css("font-size" to "18px", "padding" to "22px 24px") }
        ".faq .fold-body" style { css("padding" to "0 24px 24px") }

        // ---------- Roadmap ----------
        ".roadmap" style { css("display" to "grid", "grid-template-columns" to "repeat(3, minmax(0, 1fr))", "gap" to "20px", "position" to "relative") }
        ".milestone" style {
            css("padding" to "28px", "border-radius" to "var(--radius)", "border" to "1px solid var(--line)", "background" to "var(--card)", "display" to "grid", "gap" to "10px", "align-content" to "start")
        }
        ".milestone.now" style { css("border-color" to "var(--accent)", "background" to "linear-gradient(180deg, var(--chip), transparent 70%), var(--card)") }
        ".milestone-top" style { css("display" to "flex", "align-items" to "center", "justify-content" to "space-between", "gap" to "12px") }
        ".milestone h3" style { css("font-size" to "28px") }
        ".milestone time" style { css("font-size" to "14px", "color" to "var(--accent-text)", "font-weight" to "600") }
        ".milestone p" style { css("color" to "var(--muted)", "font-size" to "16px") }
        ".milestone .who" style { css("font-size" to "14px", "color" to "var(--faint)") }
        ".dot" style { css("width" to "10px", "height" to "10px", "border-radius" to "50%", "background" to "var(--line-strong)") }
        ".milestone.now .dot" style { css("background" to "var(--accent)", "box-shadow" to "0 0 0 6px var(--chip)") }
        ".roadmap-foot" style { css("margin-top" to "24px", "color" to "var(--muted)", "font-size" to "15px") }
        media(narrow) { ".roadmap" style { css("grid-template-columns" to "1fr") } }

        // ---------- Built in the open ----------
        ".open" style {
            css("display" to "grid", "grid-template-columns" to "minmax(0, 1.2fr) minmax(0, 1fr)", "gap" to "48px", "align-items" to "center", "padding" to "48px", "border-radius" to "28px", "border" to "1px solid var(--line-strong)", "background" to "radial-gradient(80% 120% at 100% 0%, var(--chip), transparent 60%), var(--card)")
        }
        ".open h2" style { css("font-size" to "clamp(30px, 3.6vw, 44px)") }
        ".open-copy" style { css("display" to "grid", "gap" to "16px") }
        ".open-copy p" style { css("color" to "var(--muted)", "font-size" to "17px") }
        ".open-actions" style { css("display" to "flex", "flex-wrap" to "wrap", "gap" to "12px", "margin-top" to "8px") }
        ".stack" style { css("display" to "grid", "gap" to "10px", "font-size" to "15px") }
        ".stack div" style { css("display" to "flex", "justify-content" to "space-between", "gap" to "16px", "padding" to "14px 18px", "border-radius" to "14px", "background" to "var(--bg)", "border" to "1px solid var(--line)") }
        ".stack span" style { css("color" to "var(--muted)") }
        ".stack b" style { css("font-weight" to "600", "text-align" to "right") }
        media(narrow) { ".open" style { css("grid-template-columns" to "1fr", "padding" to "32px 24px") } }

        // ---------- Closing call to action and footer ----------
        ".closer" style { css("text-align" to "center", "display" to "grid", "justify-items" to "center", "gap" to "22px") }
        ".closer h2" style { css("font-size" to "clamp(36px, 5vw, 64px)", "max-width" to "820px") }
        ".closer img" style { css("width" to "88px", "height" to "88px") }
        ".footer" style { css("padding" to "48px 0 56px", "border-top" to "1px solid var(--line)", "color" to "var(--muted)", "font-size" to "15px") }
        ".footer-inner" style { css("display" to "flex", "flex-wrap" to "wrap", "gap" to "24px 40px", "align-items" to "flex-start", "justify-content" to "space-between") }
        ".footer-links" style { css("display" to "flex", "flex-wrap" to "wrap", "gap" to "12px 28px") }
        ".footer-links a:hover" style { css("color" to "var(--text)") }
        ".footer small" style { css("display" to "block", "margin-top" to "10px", "color" to "var(--faint)", "font-size" to "13px") }

        // No entrance animation on the hero: Kobweb swaps the prerendered DOM for the live one once
        // the script loads, which would play it a second time.
        media(reducedMotion) {
            "html" style { css("scroll-behavior" to "auto") }
        }
    }
}
