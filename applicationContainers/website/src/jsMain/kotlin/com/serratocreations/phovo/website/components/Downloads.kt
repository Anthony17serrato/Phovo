package com.serratocreations.phovo.website.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.serratocreations.phovo.website.downloads.Arch
import com.serratocreations.phovo.website.downloads.Os
import com.serratocreations.phovo.website.downloads.RELEASES_URL
import com.serratocreations.phovo.website.downloads.Release
import com.serratocreations.phovo.website.downloads.ReleaseFile
import com.serratocreations.phovo.website.downloads.Target
import com.serratocreations.phovo.website.downloads.Visitor
import com.serratocreations.phovo.website.downloads.detectVisitor
import com.serratocreations.phovo.website.downloads.isExportSnapshot
import com.serratocreations.phovo.website.downloads.loadLatestRelease
import kotlinx.browser.window
import kotlin.js.Promise
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Em
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Ol
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * What the page knows about the visitor and the newest release. Both start null: that is what the
 * exported HTML shows, and what stays on screen if detection or GitHub fails, so every state below
 * still leads somewhere useful (the download list, or the releases page).
 */
class DownloadState {
    var visitor by mutableStateOf<Visitor?>(null)
    var release by mutableStateOf<Release?>(null)
    var releaseFailed by mutableStateOf(false)
    var startedTarget by mutableStateOf<Target?>(null)

    val recommended: Target? get() = visitor?.target

    fun fileFor(target: Target): ReleaseFile? = release?.file(target)
}

@Composable
fun rememberDownloadState(): DownloadState {
    val state = remember { DownloadState() }
    LaunchedEffect(Unit) {
        if (isExportSnapshot()) return@LaunchedEffect
        state.visitor = detectVisitor()
        val release = loadLatestRelease()
        state.release = release
        state.releaseFailed = release == null
    }
    return state
}

/** Social apps' built-in browsers often drop file downloads instead of handing them to the system. */
private fun inAppBrowser(): Boolean =
    Regex("Instagram|FBAN|FBAV|TikTok|LinkedInApp|Snapchat|MicroMessenger|Pinterest|Line/|Twitter", RegexOption.IGNORE_CASE)
        .containsMatchIn(window.navigator.userAgent)

private fun Target.platformIcon(): String = when (os) {
    Os.MACOS -> IconSvg.brandApple
    Os.WINDOWS -> IconSvg.brandWindows
    Os.LINUX -> if (arch == Arch.ARM64) IconSvg.brandRaspberrypi else IconSvg.brandLinux
    Os.ANDROID -> IconSvg.brandAndroid
    Os.IOS, Os.OTHER -> IconSvg.download
}

private fun Target.buttonLabel(): String = when (this) {
    Target.MAC_ARM, Target.MAC_INTEL -> "Download for Mac"
    Target.WINDOWS -> "Download for Windows"
    Target.LINUX_DEB, Target.LINUX_RPM, Target.LINUX_TAR -> "Download for Linux"
    Target.PI_DEB, Target.PI_TAR -> "Download for Raspberry Pi"
    Target.ANDROID -> "Download for Android"
}

/** The big button in the hero, and the line of detail under it. */
@Composable
fun HeroDownload(state: DownloadState) {
    val visitor = state.visitor
    val target = state.recommended
    Div({ classes("cta") }) {
        when {
            visitor?.os == Os.IOS -> IphoneNotice()
            target == null -> {
                Div({ classes("cta-row") }) {
                    A("#download", { classes("btn", "btn-primary", "btn-big") }) {
                        Icon(IconSvg.download)
                        Text("Download Phovo")
                    }
                    GithubButton()
                }
                Div({ classes("cta-meta") }) {
                    Text("Free for macOS, Windows, Linux, Raspberry Pi and Android")
                }
                visitor?.unsupportedNote?.let { note -> P({ classes("hint") }) { Text(note) } }
            }
            else -> {
                val file = state.fileFor(target)
                Div({ classes("cta-row") }) {
                    DownloadButton(state, target, file, listOf("btn", "btn-primary", "btn-big")) {
                        Icon(target.platformIcon())
                        Text(target.buttonLabel())
                    }
                    GithubButton()
                }
                Div({ classes("cta-meta") }) {
                    val variant = when (target.os) {
                        Os.MACOS -> target.title
                        Os.LINUX -> ".${target.extension}"
                        else -> null
                    }
                    val details = listOfNotNull(variant, state.release?.stageLabel, file?.sizeLabel)
                    if (details.isNotEmpty()) Span { Text(details.joinToString(" · ")) }
                    alternatesFor(visitor, target).forEach { alternate ->
                        val altFile = state.fileFor(alternate)
                        DownloadButton(state, alternate, altFile, emptyList()) {
                            Text(alternateLabel(alternate))
                        }
                    }
                    A("#download") { Text("Other platforms") }
                }
                if (target == Target.ANDROID) {
                    P({ classes("hint") }) {
                        Text("The app backs up to Phovo Desktop, so you'll also need it on a computer. ")
                        ShareToComputer()
                    }
                }
                if (target == Target.ANDROID && inAppBrowser()) {
                    P({ classes("hint") }) {
                        Text("If the download doesn't start, open this page in Chrome. Your app's built-in browser may be blocking it.")
                    }
                }
            }
        }
        state.startedTarget?.let { NextSteps(it, state) }
    }
}

private fun alternatesFor(visitor: Visitor?, target: Target): List<Target> = when {
    target.os == Os.MACOS && visitor?.archIsGuess == true ->
        listOf(if (target == Target.MAC_ARM) Target.MAC_INTEL else Target.MAC_ARM)
    target.os == Os.LINUX && target.arch == Arch.X64 ->
        listOf(Target.LINUX_DEB, Target.LINUX_RPM, Target.LINUX_TAR).filter { it != target }
    target.os == Os.LINUX && target.arch == Arch.ARM64 -> listOf(Target.PI_TAR)
    else -> emptyList()
}

private fun alternateLabel(target: Target): String = when (target) {
    Target.MAC_INTEL -> "Intel Mac?"
    Target.MAC_ARM -> "Apple Silicon Mac?"
    else -> target.extension.let { ".$it" }
}

/**
 * A real link to the file, so the browser downloads it in place. Until the release list arrives
 * (or if GitHub can't be reached) it points at the releases page instead.
 */
@Composable
private fun DownloadButton(
    state: DownloadState,
    target: Target,
    file: ReleaseFile?,
    classes: List<String>,
    content: @Composable () -> Unit,
) {
    val href = file?.url ?: state.release?.pageUrl ?: RELEASES_URL
    A(href, {
        classes(classes)
        if (file != null) {
            attr("download", file.name)
            onClick { state.startedTarget = target }
        } else {
            attr("target", "_blank")
            attr("rel", "noopener")
        }
    }) { content() }
}

@Composable
private fun GithubButton() {
    ExternalLink(com.serratocreations.phovo.website.downloads.REPO_URL, listOf("btn", "btn-ghost", "btn-big")) {
        Icon(IconSvg.brandGithub)
        Text("View on GitHub")
    }
}

@Composable
private fun IphoneNotice() {
    Div({ classes("notice") }) {
        Strong { Text("The iPhone app is on its way") }
        P {
            Text(
                "It isn't available to download yet, and it's planned for the stable release. " +
                    "You can set up the computer side now: Phovo Desktop holds your library, and it runs on " +
                    "macOS, Windows, Linux and Raspberry Pi."
            )
        }
        Div({ classes("cta-row") }) {
            ShareToComputer(asButton = true)
            ExternalLink(RELEASES_URL, listOf("link")) {
                Text("Watch releases on GitHub")
                Icon(IconSvg.externalLink)
            }
        }
    }
}

/**
 * On a phone, the useful next step is getting this page onto a computer: the share sheet (AirDrop,
 * Messages, email) where the browser has one, or copying the link where it doesn't.
 */
@Composable
private fun ShareToComputer(asButton: Boolean = false) {
    // Some in-app browsers have neither a share sheet nor a usable clipboard. Then the address is
    // shown instead, and "Link copied" only appears once the copy has actually happened.
    var result by remember { mutableStateOf(CopyResult.NONE) }
    val nav = window.navigator.asDynamic()
    val canShare = nav.share != undefined
    if (result == CopyResult.FAILED) {
        Span({ classes(if (asButton) "hint" else "link") }) { Text("Open phovo.app on your computer") }
        return
    }
    Button({
        classes(*(if (asButton) arrayOf("btn", "btn-primary", "btn-small") else arrayOf("link")))
        onClick {
            if (canShare) {
                // A dismissed share sheet rejects the promise; that's not an error worth showing.
                (nav.share(kotlin.js.json("title" to "Phovo", "url" to SITE_URL)) as Promise<Any?>).catch { }
            } else {
                val clipboard = nav.clipboard
                if (clipboard == null || clipboard == undefined) {
                    result = CopyResult.FAILED
                } else {
                    (clipboard.writeText(SITE_URL) as Promise<Any?>).then(
                        { result = CopyResult.COPIED },
                        { result = CopyResult.FAILED },
                    )
                }
            }
        }
    }) {
        Text(
            when {
                result == CopyResult.COPIED -> "Link copied"
                canShare -> "Send this page to your computer"
                else -> "Copy the link for your computer"
            }
        )
    }
}

private enum class CopyResult { NONE, COPIED, FAILED }

private const val SITE_URL = "https://phovo.app/"

/** Shown once a download starts: what to do with the file, including the unsigned-build warnings. */
@Composable
private fun NextSteps(target: Target, state: DownloadState) {
    val file = state.fileFor(target)
    Div({ classes("notice") }) {
        Strong { Text("Your download has started") }
        Ol {
            when (target.os) {
                Os.MACOS -> {
                    Li { Text("Open the .dmg and drag Phovo into Applications.") }
                    Li {
                        Text("The first time you open it, macOS says it can't verify Phovo, because alpha builds aren't signed yet. Click ")
                        B { Text("Done") }
                        Text(", not Move to Trash.")
                    }
                    Li {
                        Text("Open ")
                        B { Text("System Settings → Privacy & Security") }
                        Text(", click ")
                        B { Text("Open Anyway") }
                        Text(" next to the Phovo message, and confirm. You do this once per version.")
                    }
                }
                Os.WINDOWS -> {
                    Li { Text("Run the installer.") }
                    Li {
                        Text("Alpha builds aren't signed yet, so Windows shows \"Windows protected your PC\". Click ")
                        B { Text("More info") }
                        Text(", then ")
                        B { Text("Run anyway") }
                        Text(".")
                    }
                }
                Os.LINUX -> when (target.extension) {
                    "deb" -> Li {
                        Text("Open it in your software center, or run ")
                        Code { Text("sudo apt install ./${file?.name ?: "Phovo-*.deb"}") }
                    }
                    "rpm" -> Li {
                        Text("Open it in your software center, or run ")
                        Code { Text("sudo dnf install ./${file?.name ?: "Phovo-*.rpm"}") }
                    }
                    else -> Li {
                        Text("Unpack it anywhere and run ")
                        Code { Text("Phovo/bin/Phovo") }
                        Text(". It doesn't add a menu entry.")
                    }
                }
                Os.ANDROID -> {
                    Li { Text("Open the downloaded APK to install it. Android asks you to allow installs from your browser the first time.") }
                    Li { Text("Install Phovo Desktop on the computer that will hold your library.") }
                }
                Os.IOS, Os.OTHER -> Unit
            }
            if (target.os != Os.ANDROID) {
                Li { Text("Open Phovo on your phone, on the same Wi-Fi. Your computer shows up in the list.") }
            }
        }
    }
}

/** The full list, grouped by system, with the visitor's own card marked. */
@Composable
fun DownloadCards(state: DownloadState) {
    val mine = state.recommended
    val groups = listOf(
        Triple("macOS", IconSvg.brandApple, listOf(Target.MAC_ARM, Target.MAC_INTEL)),
        Triple("Windows", IconSvg.brandWindows, listOf(Target.WINDOWS)),
        Triple("Linux", IconSvg.brandLinux, listOf(Target.LINUX_DEB, Target.LINUX_RPM, Target.LINUX_TAR)),
        Triple("Raspberry Pi", IconSvg.brandRaspberrypi, listOf(Target.PI_DEB, Target.PI_TAR)),
        Triple("Android", IconSvg.brandAndroid, listOf(Target.ANDROID)),
    )
    Div({ classes("dl-grid") }) {
        groups.forEach { (name, icon, targets) ->
            val isMine = mine != null && mine in targets
            Div({ classes(*listOfNotNull("dl-card", "mine".takeIf { isMine }).toTypedArray()) }) {
                Div({ classes("dl-head") }) {
                    Icon(icon)
                    H3 { Text(name) }
                    if (isMine) Span({ classes("badge") }) { Text("This device") }
                }
                Div({ classes("dl-list") }) {
                    targets.forEach { target ->
                        val file = state.fileFor(target)
                        DownloadButton(state, target, file, listOf("dl-item")) {
                            B {
                                Text(target.title)
                                if (file != null) Em { Text(" · ${file.sizeLabel}") }
                            }
                            Icon(IconSvg.download)
                            Span { Text(target.detail) }
                        }
                    }
                }
            }
        }
        Div({ classes("dl-card") }) {
            Div({ classes("dl-head") }) {
                Icon(IconSvg.brandApple)
                H3 { Text("iPhone") }
                Span({ classes("badge") }) { Text("Coming") }
            }
            P { Text("The iPhone app isn't available to download yet. It's planned for the stable release.") }
        }
    }
    Div({ classes("dl-foot") }) {
        val release = state.release
        when {
            release != null -> Span { Text("Phovo ${release.version}${if (release.isPrerelease) ", a pre-release" else ""}") }
            state.releaseFailed -> Span { Text("Couldn't reach GitHub just now, so the links open the releases page.") }
        }
        ExternalLink(release?.pageUrl ?: RELEASES_URL, listOf("link")) {
            Text("Release notes")
            Icon(IconSvg.externalLink)
        }
        release?.checksumsUrl?.let { url ->
            A(url, { classes("link"); attr("download", "SHA256SUMS") }) { Text("SHA256SUMS") }
        }
        ExternalLink(RELEASES_URL, listOf("link")) {
            Text("All releases")
            Icon(IconSvg.externalLink)
        }
    }
    Div({ classes("dl-note") }) {
        FirstLaunchFold(
            "Opening Phovo on a Mac for the first time",
            open = mine?.os == Os.MACOS,
        ) {
            P { Text("Alpha builds aren't code signed yet, so macOS shows \"Phovo\" Not Opened.") }
            Ol {
                Li { Text("Click "); Strong { Text("Done") }; Text(", not Move to Trash.") }
                Li { Text("Open "); Strong { Text("System Settings → Privacy & Security") }; Text(" and scroll down to Security.") }
                Li { Text("Click "); Strong { Text("Open Anyway") }; Text(" next to the Phovo message, confirm with your password or Touch ID, then click "); Strong { Text("Open") }; Text(".") }
            }
            P { Text("You need to do this once per version.") }
        }
        FirstLaunchFold(
            "Opening Phovo on Windows for the first time",
            open = mine?.os == Os.WINDOWS,
        ) {
            P { Text("Alpha builds aren't code signed yet, so SmartScreen shows \"Windows protected your PC\".") }
            Ol {
                Li { Text("Click "); Strong { Text("More info") }; Text(".") }
                Li { Text("Click "); Strong { Text("Run anyway") }; Text(".") }
            }
        }
    }
}

@Composable
private fun FirstLaunchFold(title: String, open: Boolean, content: @Composable () -> Unit) {
    Details({
        classes("fold")
        if (open) attr("open", "")
    }) {
        Summary {
            Icon(IconSvg.shieldCheck)
            Text(title)
            Icon(IconSvg.chevronDown)
        }
        Div({ classes("fold-body") }) { content() }
    }
}
