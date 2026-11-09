package com.serratocreations.phovo.website.components

import androidx.compose.runtime.Composable
import com.serratocreations.phovo.website.downloads.NEW_ISSUE_URL
import com.serratocreations.phovo.website.downloads.RELEASES_URL
import com.serratocreations.phovo.website.downloads.REPO_URL
import com.serratocreations.phovo.website.downloads.SECURITY_ISSUE_URL
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Em
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.jetbrains.compose.web.dom.Video
import org.w3c.dom.HTMLVideoElement

@Composable
fun SiteNav() {
    Header({ classes("nav") }) {
        Div({ classes("container", "nav-inner") }) {
            Brand()
            Nav({ classes("nav-links"); attr("aria-label", "Sections") }) {
                A("#how") { Text("How it works") }
                A("#features") { Text("Features") }
                A("#download") { Text("Download") }
                A("#roadmap") { Text("Roadmap") }
                A("#faq") { Text("FAQ") }
            }
            Div({ classes("nav-actions") }) {
                ExternalLink(REPO_URL, listOf("btn", "btn-ghost", "btn-small")) {
                    Icon(IconSvg.brandGithub)
                    Span({ classes("nav-github-label") }) { Text("GitHub") }
                }
                A("#download", { classes("btn", "btn-primary", "btn-small") }) { Text("Download") }
            }
        }
    }
}

@Composable
fun Hero(downloads: DownloadState) {
    Section({ classes("hero") }) {
        Div({ classes("glow"); attr("aria-hidden", "true") }) { repeat(4) { Span() } }
        Div({ classes("container", "hero-grid") }) {
            Div({ classes("hero-copy") }) {
                val release = downloads.release
                ExternalLink(release?.pageUrl ?: RELEASES_URL, listOf("pill")) {
                    B { Text("Alpha") }
                    Text(if (release != null) "Phovo ${release.version} is out" else "The first test builds are out")
                    Icon(IconSvg.arrowRight)
                }
                H1 {
                    Text("Your photos, backed up to a computer ")
                    Em { Text("you own.") }
                }
                P({ classes("lede") }) {
                    Text(
                        "Phovo turns the desktop or laptop you already have into a private photo cloud. " +
                            "Your phone backs up to it over Wi-Fi, at full quality, with no subscription."
                    )
                }
                HeroDownload(downloads)
                Div({ classes("trust") }) {
                    TrustItem("No subscription")
                    TrustItem("No account to create")
                    TrustItem("Originals at full resolution")
                    TrustItem("Stays on your network")
                }
            }
            Devices()
        }
    }
}

@Composable
private fun TrustItem(label: String) {
    Span {
        Icon(IconSvg.check)
        Text(label)
    }
}

@Composable
private fun Devices() {
    Div({ classes("devices-wrap") }) {
        DevicesComposition()
        P({ classes("devices-caption") }) { Text("Shown on iPhone, coming soon. Android is available now.") }
    }
}

@Composable
private fun DevicesComposition() {
    Div({ classes("devices") }) {
        Div({ classes("window") }) {
            Img("/media/desktop-timeline-1000.webp", "Phovo Desktop showing a photo library as a timeline, grouped by month") {
                attr("srcset", "/media/desktop-timeline-1000.webp 1000w, /media/desktop-timeline-1800.webp 1800w")
                attr("sizes", "(max-width: 960px) 92vw, 560px")
                attr("width", "1000")
                attr("height", "622")
                attr("fetchpriority", "high")
            }
        }
        Div({ classes("phone") }) {
            WalkthroughVideo()
        }
    }
}

/** The README's walkthrough GIF, re-encoded as a 1.5 MB MP4 (the GIF is 14 MB). */
@Composable
private fun WalkthroughVideo() {
    Video({
        attr("poster", "/media/ios-walkthrough-poster.webp")
        attr("width", "480")
        attr("height", "1038")
        attr("muted", "")
        attr("loop", "")
        attr("playsinline", "")
        attr("preload", "metadata")
        attr("aria-label", "Phovo on iPhone: onboarding, finding a server on the network and backing up")
        attr("src", "/media/ios-walkthrough.mp4")
        ref { element ->
            val video = element as HTMLVideoElement
            // `muted` has to be set as a property for autoplay; the attribute alone only sets the default.
            video.muted = true
            val reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches
            if (reduceMotion) {
                video.controls = true
            } else {
                video.autoplay = true
                video.play().catch { }
            }
            onDispose { }
        }
    })
}

@Composable
fun HowItWorks() {
    Section({ id("how"); classes("section") }) {
        Div({ classes("container") }) {
            SectionHead(
                eyebrow = "How it works",
                title = "Your computer becomes the cloud",
                body = "Phovo Desktop runs a small backup server on your computer. The phone app finds it on your network and sends your photos and videos there.",
                centered = true,
            )
            Div({ classes("flow"); attr("aria-hidden", "true") }) {
                Div({ classes("flow-node") }) { Icon(IconSvg.smartphone) }
                Div({ classes("flow-line") }) { repeat(3) { I() } }
                Div({ classes("flow-wifi") }) { Icon(IconSvg.wifi) }
                Div({ classes("flow-line") }) { repeat(3) { I() } }
                Div({ classes("flow-node") }) { Icon(IconSvg.laptop) }
            }
            Div({ classes("steps") }) {
                Step(1, "Install Phovo Desktop", "Put it on the computer that will hold your library. It hosts the backup server.")
                Step(2, "Open Phovo on your phone", "Allow it access to your photo library.")
                Step(3, "Connect", "Phovo lists the servers it finds on your network. Tap yours and your library starts uploading.")
            }
        }
    }
}

@Composable
private fun Step(number: Int, title: String, body: String) {
    Div({ classes("step") }) {
        Span({ classes("step-num") }) { Text(number.toString()) }
        H3 { Text(title) }
        P { Text(body) }
    }
}

@Composable
fun Features() {
    Section({ id("features"); classes("section", "section-alt") }) {
        Div({ classes("container") }) {
            SectionHead(
                eyebrow = "Features",
                title = "Backups you don't have to babysit",
            )
            Div({ classes("showcase") }) {
                Div({ classes("show") }) {
                    Div({ classes("show-media") }) {
                        Shot("/media/ios-connect-to-server.webp", "Phovo on iPhone listing a computer it found on the network", 600, 1299)
                    }
                    Div({ classes("show-copy") }) {
                        Span({ classes("eyebrow") }) { Text("Discovery") }
                        H3 { Text("It finds your computer for you") }
                        P {
                            Text(
                                "Open the app on the same Wi-Fi as Phovo Desktop and your computer shows up in the list, " +
                                    "ready to tap. If your network blocks discovery, you can type its address in instead."
                            )
                        }
                    }
                }
                Div({ classes("show", "flip") }) {
                    Div({ classes("show-media") }) {
                        Shot("/media/android-backup-progress.webp", "Phovo on Android showing a backup in progress", 600, 1335)
                    }
                    Div({ classes("show-copy") }) {
                        Span({ classes("eyebrow") }) { Text("Background backup") }
                        H3 { Text("Uploads keep going after you leave the app") }
                        P {
                            Text(
                                "Start a backup and switch to something else. Phovo carries on uploading in the " +
                                    "background and shows you how far along it is."
                            )
                        }
                    }
                }
                Div({ classes("show") }) {
                    Div({ classes("show-media", "wide") }) {
                        Shot("/media/desktop-timeline-1000.webp", "Phovo Desktop showing the backed-up library grouped by month", 1000, 622)
                    }
                    Div({ classes("show-copy") }) {
                        Span({ classes("eyebrow") }) { Text("Phovo Desktop") }
                        H3 { Text("Your whole library on the big screen") }
                        P { Text("Phovo Desktop shows everything your phone has backed up as one timeline, grouped by month, and plays your videos right there.") }
                        Ul({ classes("points") }) {
                            Point("Photos and videos at their original resolution")
                            Point("Runs on macOS, Windows, Linux and a 64-bit Raspberry Pi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Point(text: String) {
    Li {
        Icon(IconSvg.check)
        Span { Text(text) }
    }
}

@Composable
fun Why() {
    Section({ id("why"); classes("section") }) {
        Div({ classes("container") }) {
            SectionHead(
                eyebrow = "Why Phovo",
                title = "Cloud storage without the monthly bill",
                body = "Cloud photo plans charge you every month for space on someone else's servers, and the bill grows with your library. Phovo keeps your library on your own computer.",
            )
            Div({ classes("why") }) {
                WhyCard(IconSvg.hardDrive, "Storage that grows with your drive", "There's no quota and no storage plan. You pick the folder or drive your library lives on, so how big it gets is up to you.")
                WhyCard(IconSvg.image, "Originals, untouched", "Photos and videos are stored at their original resolution, with no cloud compression.")
                WhyCard(IconSvg.shieldCheck, "Private by design", "Your media travels from your phone to your computer over your own network, and no company in between sees it.")
            }
        }
    }
}

@Composable
private fun WhyCard(icon: String, title: String, body: String) {
    Div({ classes("why-card") }) {
        Div({ classes("why-icon") }) { Icon(icon) }
        H3 { Text(title) }
        P { Text(body) }
    }
}

@Composable
fun Gallery() {
    Section({ classes("section", "section-alt") }) {
        Div({ classes("container") }) {
            SectionHead(
                eyebrow = "A look around",
                title = "One app on every device",
                body = "Android, macOS, Windows and Linux all run the same Phovo, down to a Raspberry Pi. The iPhone app is on its way.",
            )
        }
        Div({ classes("gallery") }) {
            GalleryShot("/media/android-welcome.webp", "Android: welcome and photo access", 1336)
            GalleryShot("/media/ios-timeline.webp", "iPhone (coming soon): photo timeline", 1299)
            GalleryShot("/media/ios-connections.webp", "iPhone (coming soon): connections and setup", 1299)
            GalleryShot("/media/android-details.webp", "Android: photo details", 1336)
        }
        Div({ classes("container") }) {
            Div({ classes("platforms") }) {
                Platform(IconSvg.brandApple, "macOS", "Apple Silicon and Intel")
                Platform(IconSvg.brandWindows, "Windows", "10 and 11")
                Platform(IconSvg.brandLinux, "Linux", ".deb, .rpm and .tar.gz")
                Platform(IconSvg.brandRaspberrypi, "Raspberry Pi", "64-bit Raspberry Pi OS")
                Platform(IconSvg.brandAndroid, "Android", "7.0 and later")
                Platform(IconSvg.brandApple, "iPhone", "Coming soon", soon = true)
            }
        }
    }
}

@Composable
private fun GalleryShot(src: String, caption: String, height: Int) {
    Figure {
        Shot(src, caption, 600, height, className = "gallery-shot")
        Figcaption { Text(caption) }
    }
}

@Composable
private fun Platform(icon: String, name: String, detail: String, soon: Boolean = false) {
    Div({ classes(*listOfNotNull("platform", "soon".takeIf { soon }).toTypedArray()) }) {
        Icon(icon)
        Span { Text(name) }
        Small { Text(detail) }
    }
}

@Composable
fun DownloadSection(downloads: DownloadState) {
    Section({ id("download"); classes("section") }) {
        Div({ classes("container") }) {
            SectionHead(
                eyebrow = "Download",
                title = "Get Phovo",
                body = "Start with Phovo Desktop on the computer that will keep your library, then add the app to your phone. " +
                    "Phovo is in alpha: expect bugs, and expect breaking changes between releases until the beta.",
            )
            DownloadCards(downloads)
        }
    }
}

@Composable
fun Roadmap() {
    Section({ id("roadmap"); classes("section", "section-alt") }) {
        Div({ classes("container") }) {
            SectionHead(
                eyebrow = "Roadmap",
                title = "Where Phovo is headed",
                body = "The goal is a full replacement for Google Photos and Apple Photos that runs on hardware you own.",
            )
            Div({ classes("roadmap") }) {
                Milestone(
                    name = "Alpha",
                    date = "Out now",
                    who = "For early testers",
                    body = "Core features under test. Expect bugs, instability and breaking changes with no backward compatibility.",
                    now = true,
                )
                Milestone(
                    name = "Beta",
                    date = "Planned for Feb 28, 2027",
                    who = "For everyone",
                    body = "Initial features complete and no more breaking changes. Work shifts to stability and the remaining bugs.",
                )
                Milestone(
                    name = "Stable",
                    date = "Planned for May 31, 2027",
                    who = "On Android, iPhone and desktop",
                    body = "Production ready, with every critical and high-priority bug fixed.",
                )
            }
            P({ classes("roadmap-foot") }) {
                Text("Wear OS, Android XR and Android TV apps may follow if there's demand for them.")
            }
        }
    }
}

@Composable
private fun Milestone(name: String, date: String, who: String, body: String, now: Boolean = false) {
    Div({ classes(*listOfNotNull("milestone", "now".takeIf { now }).toTypedArray()) }) {
        Div({ classes("milestone-top") }) {
            H3 { Text(name) }
            Span({ classes("dot") })
        }
        Time { Text(date) }
        P { Text(body) }
        Span({ classes("who") }) { Text(who) }
    }
}

@Composable
fun BuiltInTheOpen() {
    Section({ id("code"); classes("section") }) {
        Div({ classes("container") }) {
            Div({ classes("open") }) {
                Div({ classes("open-copy") }) {
                    Span({ classes("eyebrow") }) { Text("Built in the open") }
                    H2 { Text("The code is on GitHub") }
                    P {
                        Text(
                            "Every Phovo app comes from one Kotlin Multiplatform codebase. The logic is shared across " +
                                "Android, iOS and desktop, and the interface is written once in Compose Multiplatform. " +
                                "Read the code, follow development, or open an issue when something breaks."
                        )
                    }
                    Div({ classes("open-actions") }) {
                        ExternalLink(REPO_URL, listOf("btn", "btn-primary")) {
                            Icon(IconSvg.brandGithub)
                            Text("View on GitHub")
                        }
                        ExternalLink(NEW_ISSUE_URL, listOf("btn", "btn-ghost")) {
                            Text("Report a bug")
                        }
                    }
                }
                Div({ classes("stack") }) {
                    StackRow("Language", "Kotlin Multiplatform")
                    StackRow("Interface", "Compose Multiplatform")
                    StackRow("Backup server", "Ktor, inside Phovo Desktop")
                    StackRow("Database", "Room")
                    StackRow("This website", "Kobweb, also Kotlin")
                }
            }
        }
    }
}

@Composable
private fun StackRow(label: String, value: String) {
    Div {
        Span { Text(label) }
        B { Text(value) }
    }
}

@Composable
fun Faq() {
    Section({ id("faq"); classes("section", "section-alt") }) {
        Div({ classes("container") }) {
            SectionHead(eyebrow = "FAQ", title = "Questions people ask", centered = true)
            Div({ classes("faq") }) {
                Question("What does Phovo cost?") {
                    P { Text("Nothing. There's no subscription and no storage plan. Your library lives on your own disk, so the only limit is the size of the drive.") }
                }
                Question("What do I need?") {
                    P {
                        Text(
                            "A computer to keep your library on, running Phovo Desktop. That can be a Mac, a Windows PC, a Linux " +
                                "machine, or a Raspberry Pi 3, 4, 5 or Zero 2 W on 64-bit Raspberry Pi OS. Then the Phovo app on your " +
                                "Android phone. The iPhone app isn't available to download yet."
                        )
                    }
                }
                Question("Does my computer have to stay on?") {
                    P {
                        Text(
                            "Yes, while you want backups to run. Your phone backs up when the computer is awake with Phovo Desktop " +
                                "running, and while the phone is on the same network as the computer."
                        )
                    }
                }
                Question("Is my library protected on my network?") {
                    P {
                        Text(
                            "Not fully yet. Phovo sends your photos over your local network without encryption, and Phovo " +
                                "Desktop doesn't ask devices to sign in, so any device on the same network can reach it. Use it " +
                                "on a network you trust, like your home Wi-Fi. Encryption and device sign-in are planned: follow "
                        )
                        ExternalLink(SECURITY_ISSUE_URL, listOf("link")) { Text("issue #134") }
                        Text(" on GitHub.")
                    }
                }
                Question("How is Phovo different from Immich or PhotoPrism?") {
                    P {
                        Text(
                            "Immich and PhotoPrism are capable tools that you set up the way a server admin would, with Docker, " +
                                "the command line and config files. Phovo installs like any other app. Put Phovo Desktop on your " +
                                "computer, open Phovo on your phone and tap your computer, and your backup starts. There's nothing to configure."
                        )
                    }
                }
                Question("Should Phovo be the only copy of my photos?") {
                    P {
                        Text(
                            "Not yet. Phovo is in alpha, so expect bugs, and expect breaking changes between releases until the " +
                                "beta. Keep your current backup while you try it. Even after that, one computer in the same house as your " +
                                "phone isn't a complete backup on its own, so keep a copy of your library somewhere else too."
                        )
                    }
                }
                Question("Why does my Mac or PC warn me when I open it?") {
                    P {
                        Text("The desktop builds aren't code signed yet. The ")
                        A("#download", { classes("link") }) { Text("download section") }
                        Text(" has the steps to open Phovo anyway on macOS and on Windows.")
                    }
                }
                Question("Where do I report a bug?") {
                    P {
                        Text("Open an issue on ")
                        ExternalLink(NEW_ISSUE_URL, listOf("link")) { Text("GitHub") }
                        Text(". Say which device and which version you're on; the About screen in the overflow menu shows the version.")
                    }
                }
            }
        }
    }
}

@Composable
private fun Question(title: String, answer: @Composable () -> Unit) {
    Details({ classes("fold") }) {
        Summary {
            Text(title)
            Icon(IconSvg.chevronDown)
        }
        Div({ classes("fold-body") }) { answer() }
    }
}

@Composable
fun Closer() {
    Section({ classes("section") }) {
        Div({ classes("container", "closer") }) {
            Img("/icon-192.png", alt = "") {
                attr("width", "88")
                attr("height", "88")
                attr("loading", "lazy")
            }
            H2 { Text("Give your photos a home you own.") }
            Div({ classes("cta-row") }) {
                A("#download", { classes("btn", "btn-primary", "btn-big") }) {
                    Icon(IconSvg.download)
                    Text("Download Phovo")
                }
                ExternalLink(REPO_URL, listOf("btn", "btn-ghost", "btn-big")) {
                    Icon(IconSvg.brandGithub)
                    Text("View on GitHub")
                }
            }
        }
    }
}

@Composable
fun SiteFooter() {
    Footer({ classes("footer") }) {
        Div({ classes("container", "footer-inner") }) {
            Div {
                Brand()
                Small { Text("Your photos and videos, backed up to a computer you own.") }
            }
            Nav({ classes("footer-links"); attr("aria-label", "Footer") }) {
                A("#download") { Text("Download") }
                A("#roadmap") { Text("Roadmap") }
                ExternalLink(REPO_URL) { Text("GitHub") }
                ExternalLink(RELEASES_URL) { Text("Releases") }
                ExternalLink(NEW_ISSUE_URL) { Text("Report a bug") }
            }
            Div {
                Small { Text("© 2026 Phovo. This site sets no cookies and runs no analytics.") }
            }
        }
    }
}
