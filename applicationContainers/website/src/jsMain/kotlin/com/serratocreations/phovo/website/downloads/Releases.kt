package com.serratocreations.phovo.website.downloads

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.RequestInit
import kotlin.js.Date
import kotlin.js.json

const val REPO_URL = "https://github.com/Anthony17serrato/Phovo"
const val RELEASES_URL = "$REPO_URL/releases"
const val NEW_ISSUE_URL = "$REPO_URL/issues/new"
const val SECURITY_ISSUE_URL = "$REPO_URL/issues/134"

private const val RELEASES_API =
    "https://api.github.com/repos/Anthony17serrato/Phovo/releases?per_page=10"
private const val CACHE_KEY = "phovo.release.v1"
private const val CACHE_MILLIS = 10 * 60 * 1000

enum class Os { MACOS, WINDOWS, LINUX, ANDROID, IOS, OTHER }
enum class Arch { ARM64, X64 }

/**
 * One kind of file the release workflow publishes. The site never names a version: it reads the
 * newest release from GitHub at runtime and matches its files against these by the
 * `Phovo-<version>-<os>[-<arch>].<ext>` naming that `release.yml` uses.
 */
enum class Target(
    val os: Os,
    val arch: Arch?,
    val extension: String,
    val title: String,
    val detail: String,
) {
    MAC_ARM(Os.MACOS, Arch.ARM64, "dmg", "Apple Silicon", "M1 and later"),
    MAC_INTEL(Os.MACOS, Arch.X64, "dmg", "Intel", "Macs with an Intel processor"),
    WINDOWS(Os.WINDOWS, Arch.X64, "msi", "Windows 10 and 11", "Also runs on Windows on ARM, through emulation"),
    LINUX_DEB(Os.LINUX, Arch.X64, "deb", ".deb", "Ubuntu, Debian, Linux Mint, Pop!_OS"),
    LINUX_RPM(Os.LINUX, Arch.X64, "rpm", ".rpm", "Fedora, RHEL, Rocky, AlmaLinux, openSUSE"),
    LINUX_TAR(Os.LINUX, Arch.X64, "tar.gz", ".tar.gz", "Arch, Manjaro and other distros. Unpack it and run Phovo/bin/Phovo"),
    PI_DEB(Os.LINUX, Arch.ARM64, "deb", ".deb", "Raspberry Pi 3, 4, 5 or Zero 2 W on 64-bit Raspberry Pi OS"),
    PI_TAR(Os.LINUX, Arch.ARM64, "tar.gz", ".tar.gz", "Other 64-bit ARM Linux. Unpack it and run Phovo/bin/Phovo"),
    ANDROID(Os.ANDROID, null, "apk", "Android 7.0 and later", "Install the APK on your phone"),
    ;

    val osLabel: String
        get() = when (os) {
            Os.MACOS -> "macOS"
            Os.WINDOWS -> "Windows"
            Os.LINUX -> if (arch == Arch.ARM64) "Raspberry Pi" else "Linux"
            Os.ANDROID -> "Android"
            Os.IOS -> "iPhone"
            Os.OTHER -> "your computer"
        }

    internal fun matches(os: String, arch: String?, extension: String): Boolean =
        os == this.os.name.lowercase() &&
            (this.arch == null || arch == this.arch.name.lowercase()) &&
            extension == this.extension
}

class ReleaseFile(val target: Target, val name: String, val url: String, val sizeBytes: Double) {
    val sizeLabel: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return "${mb.toInt()} MB"
        }
}

class Release(
    val version: String,
    val pageUrl: String,
    val isPrerelease: Boolean,
    val files: List<ReleaseFile>,
    val checksumsUrl: String?,
) {
    fun file(target: Target): ReleaseFile? = files.firstOrNull { it.target == target }

    /** "1.0.0-alpha.1" → "Alpha 1", "1.2.0-beta.3" → "Beta 3", "1.0.0" → "1.0.0". */
    val stageLabel: String
        get() {
            val match = Regex("""-(alpha|beta|rc)\.(\d+)$""").find(version) ?: return version
            val (stage, number) = match.destructured
            val name = if (stage == "rc") "Release candidate" else stage.replaceFirstChar { it.uppercase() }
            return "$name $number"
        }
}

private val fileName = Regex("""^Phovo-(.+)-(android|macos|windows|linux)(?:-(arm64|x64))?\.(apk|dmg|msi|deb|rpm|tar\.gz)$""")

/**
 * The newest published release, pre-releases included (GitHub's "latest" skips those, and every
 * build is a pre-release until 1.0). Null when GitHub can't be reached or rate-limits the visitor;
 * the page then links to the releases page instead.
 */
suspend fun loadLatestRelease(): Release? {
    readCache()?.let { return parseReleases(it) }
    return try {
        val init = RequestInit(headers = json("Accept" to "application/vnd.github+json"))
        val response = window.fetch(RELEASES_API, init).await()
        if (!response.ok) return null
        val text = response.text().await()
        writeCache(text)
        parseReleases(text)
    } catch (e: Throwable) {
        null
    }
}

private fun parseReleases(text: String): Release? {
    val releases = JSON.parse<Array<dynamic>>(text)
    val newest = releases.firstOrNull { it.draft != true && (it.assets as Array<dynamic>).isNotEmpty() }
        ?: return null
    val assets = newest.assets as Array<dynamic>
    val files = assets.mapNotNull { asset ->
        val name = asset.name as String
        val (_, os, arch, ext) = fileName.find(name)?.destructured ?: return@mapNotNull null
        val target = Target.entries.firstOrNull { it.matches(os, arch.ifEmpty { null }, ext) }
            ?: return@mapNotNull null
        ReleaseFile(target, name, asset.browser_download_url as String, (asset.size as Number).toDouble())
    }
    val tag = newest.tag_name as String
    return Release(
        version = tag.removePrefix("v"),
        pageUrl = newest.html_url as String,
        isPrerelease = newest.prerelease == true,
        files = files,
        checksumsUrl = assets.firstOrNull { it.name == "SHA256SUMS" }?.browser_download_url as String?,
    )
}

// A short session cache, so moving around the page or reloading it doesn't spend the visitor's
// 60-requests-an-hour allowance on GitHub's unauthenticated API.
private fun readCache(): String? = try {
    val raw = window.sessionStorage.getItem(CACHE_KEY)
    val savedAt = window.sessionStorage.getItem("$CACHE_KEY.at")?.toDoubleOrNull()
    if (raw != null && savedAt != null && Date.now() - savedAt < CACHE_MILLIS) raw else null
} catch (e: Throwable) {
    null
}

private fun writeCache(text: String) {
    try {
        window.sessionStorage.setItem(CACHE_KEY, text)
        window.sessionStorage.setItem("$CACHE_KEY.at", Date.now().toString())
    } catch (e: Throwable) {
        // Storage can be off (private mode, blocked site data). The page works without it.
    }
}
