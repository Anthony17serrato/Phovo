import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.unsafe

// phovo.app: a Kobweb site (Compose HTML on Kotlin/JS). The landing page shares no UI with the
// apps, so it renders real HTML instead of a Compose Multiplatform canvas: static export prerenders
// every page, which keeps it crawlable, gives link previews something to read and paints before
// any Kotlin runs.
//
// Export the site (output in .kobweb/site, which firebase.json serves):
//   ./gradlew :applicationContainers:website:kobwebExport -PkobwebReuseServer=false -PkobwebEnv=DEV \
//     -PkobwebRunLayout=STATIC -PkobwebBuildTarget=RELEASE -PkobwebExportLayout=STATIC
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kobweb.application)
}

group = "com.serratocreations.phovo.website"

kobweb {
    app {
        index {
            faviconPath.set("/icon-32.png")
            description.set(
                "Phovo turns the computer you already have into a private photo cloud. " +
                    "Your phone backs up to it over Wi-Fi, at full quality, with no subscription."
            )
            head.add {
                val title = "Phovo: back up your phone to a computer you own"
                val summary = "Phovo turns the computer you already have into a private photo cloud. " +
                    "Your phone backs up to it over Wi-Fi, at full quality, with no subscription."
                meta(name = "theme-color", content = "#0E1513")
                meta(name = "color-scheme", content = "dark light")
                link(rel = "canonical", href = "https://phovo.app/")
                link(rel = "icon", type = "image/png", href = "/icon-192.png") { attributes["sizes"] = "192x192" }
                link(rel = "apple-touch-icon", href = "/icon-180.png")
                link(rel = "preload", href = "/fonts/fraunces-display.woff2") {
                    attributes["as"] = "font"
                    attributes["type"] = "font/woff2"
                    attributes["crossorigin"] = ""
                }
                // Link previews (iMessage, Slack, Discord, X, LinkedIn) read these without running any script.
                meta { attributes["property"] = "og:type"; content = "website" }
                meta { attributes["property"] = "og:site_name"; content = "Phovo" }
                meta { attributes["property"] = "og:url"; content = "https://phovo.app/" }
                meta { attributes["property"] = "og:title"; content = title }
                meta { attributes["property"] = "og:description"; content = summary }
                meta { attributes["property"] = "og:image"; content = "https://phovo.app/og.jpg" }
                meta { attributes["property"] = "og:image:width"; content = "1200" }
                meta { attributes["property"] = "og:image:height"; content = "630" }
                meta(name = "twitter:card", content = "summary_large_image")
                meta(name = "twitter:title", content = title)
                meta(name = "twitter:description", content = summary)
                meta(name = "twitter:image", content = "https://phovo.app/og.jpg")
                script(type = "application/ld+json") {
                    unsafe {
                        +"""
                        {"@context":"https://schema.org","@type":"SoftwareApplication","name":"Phovo",
                         "url":"https://phovo.app/","description":"$summary",
                         "applicationCategory":"MultimediaApplication",
                         "operatingSystem":"Android, macOS, Windows, Linux",
                         "offers":{"@type":"Offer","price":"0","priceCurrency":"USD"},
                         "codeRepository":"https://github.com/Anthony17serrato/Phovo"}
                        """.trimIndent()
                    }
                }
            }
        }
    }
}

kotlin {
    configAsKobwebApplication("website")

    // Unit tests run on Node, so neither CI nor a developer machine needs a browser for them.
    js {
        nodejs()
        browser {
            testTask { enabled = false }
        }
    }

    sourceSets {
        jsMain.dependencies {
            implementation(libs.kobweb.compose.runtime)
            implementation(libs.compose.html.core)
            implementation(libs.kobweb.core)
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
