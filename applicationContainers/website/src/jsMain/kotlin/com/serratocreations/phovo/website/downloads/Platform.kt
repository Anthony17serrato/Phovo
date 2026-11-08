package com.serratocreations.phovo.website.downloads

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.dom.HTMLCanvasElement
import kotlin.js.Promise

/**
 * A best guess at the visitor's device. [archIsGuess] is true when nothing in the browser said
 * which processor a Mac has, so the page also offers the other build next to the recommended one.
 */
class Visitor(val os: Os, val arch: Arch?, val archIsGuess: Boolean, val target: Target?)

/**
 * True while Kobweb's export renders the page in headless Chromium. The exported HTML must not
 * bake in that browser's platform (Linux x64) or one moment's release list, so detection only runs
 * in a real visitor's browser and the exported page keeps its neutral "Download" state.
 */
fun isExportSnapshot(): Boolean = window.navigator.asDynamic().webdriver == true

/**
 * `?device=iphone`, `android`, `mac`, `mac-intel`, `windows`, `linux`, `fedora`, `pi` or `other`
 * shows the page as that device would see it, for checking each download path from one browser.
 */
private fun deviceOverride(): Visitor? = when (org.w3c.dom.url.URLSearchParams(window.location.search).get("device")) {
    "iphone" -> Visitor(Os.IOS, null, archIsGuess = false, null)
    "android" -> Visitor(Os.ANDROID, null, archIsGuess = false, Target.ANDROID)
    "mac" -> Visitor(Os.MACOS, Arch.ARM64, archIsGuess = true, Target.MAC_ARM)
    "mac-intel" -> Visitor(Os.MACOS, Arch.X64, archIsGuess = false, Target.MAC_INTEL)
    "windows" -> Visitor(Os.WINDOWS, Arch.X64, archIsGuess = false, Target.WINDOWS)
    "linux" -> Visitor(Os.LINUX, Arch.X64, archIsGuess = false, Target.LINUX_DEB)
    "fedora" -> Visitor(Os.LINUX, Arch.X64, archIsGuess = false, Target.LINUX_RPM)
    "pi" -> Visitor(Os.LINUX, Arch.ARM64, archIsGuess = false, Target.PI_DEB)
    "other" -> Visitor(Os.OTHER, null, archIsGuess = false, null)
    else -> null
}

suspend fun detectVisitor(): Visitor {
    deviceOverride()?.let { return it }
    val ua = window.navigator.userAgent
    val touchPoints = (window.navigator.asDynamic().maxTouchPoints as? Number)?.toInt() ?: 0
    val hints = highEntropyArchitecture()

    val os = when {
        Regex("iPhone|iPad|iPod").containsMatchIn(ua) -> Os.IOS
        // iPadOS asks for the desktop site by default and then reports itself as a Mac.
        ua.contains("Macintosh") && touchPoints > 1 -> Os.IOS
        ua.contains("Android") -> Os.ANDROID
        ua.contains("Windows") -> Os.WINDOWS
        ua.contains("CrOS") -> Os.OTHER
        ua.contains("Macintosh") || ua.contains("Mac OS X") -> Os.MACOS
        ua.contains("Linux") || ua.contains("X11") -> Os.LINUX
        else -> Os.OTHER
    }

    return when (os) {
        Os.MACOS -> {
            val arch = hints ?: macArchFromGpu()
            val resolved = arch ?: Arch.ARM64 // Every Mac sold since 2023 is Apple Silicon.
            Visitor(os, resolved, archIsGuess = arch == null, if (resolved == Arch.ARM64) Target.MAC_ARM else Target.MAC_INTEL)
        }
        Os.LINUX -> {
            val platform = window.navigator.platform
            val isArm = hints == Arch.ARM64 || Regex("aarch64|arm64|armv8", RegexOption.IGNORE_CASE).containsMatchIn("$ua $platform")
            val target = when {
                isArm -> Target.PI_DEB
                Regex("Fedora|Red Hat|CentOS|Rocky|SUSE", RegexOption.IGNORE_CASE).containsMatchIn(ua) -> Target.LINUX_RPM
                else -> Target.LINUX_DEB
            }
            Visitor(os, if (isArm) Arch.ARM64 else Arch.X64, archIsGuess = false, target)
        }
        Os.WINDOWS -> Visitor(os, Arch.X64, archIsGuess = false, Target.WINDOWS)
        Os.ANDROID -> Visitor(os, null, archIsGuess = false, Target.ANDROID)
        Os.IOS, Os.OTHER -> Visitor(os, null, archIsGuess = false, null)
    }
}

/** Chromium browsers report the CPU architecture through User-Agent Client Hints. */
private suspend fun highEntropyArchitecture(): Arch? = try {
    val data = window.navigator.asDynamic().userAgentData
    if (data == null || data == undefined) {
        null
    } else {
        val values = (data.getHighEntropyValues(arrayOf("architecture")) as Promise<dynamic>).await()
        when (values.architecture as? String) {
            "arm" -> Arch.ARM64
            "x86" -> Arch.X64
            else -> null
        }
    }
} catch (e: Throwable) {
    null
}

/**
 * Safari and Firefox don't share the architecture, and Safari on Apple Silicon still says
 * "Intel Mac OS X". The GPU name gives it away when the browser exposes it: "Apple M2" on Apple
 * Silicon, an Intel, AMD or NVIDIA part on an Intel Mac. Safari only says "Apple GPU", which
 * isn't enough to tell, so that returns null.
 */
private fun macArchFromGpu(): Arch? = try {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    val gl = canvas.getContext("webgl")?.asDynamic()
    if (gl == null) {
        null
    } else {
        val ext = gl.getExtension("WEBGL_debug_renderer_info")
        val renderer = (if (ext != null) gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) else gl.getParameter(gl.RENDERER)) as? String
        when {
            renderer == null -> null
            Regex("""Apple M\d""").containsMatchIn(renderer) -> Arch.ARM64
            Regex("Intel|AMD|Radeon|NVIDIA", RegexOption.IGNORE_CASE).containsMatchIn(renderer) -> Arch.X64
            else -> null
        }
    }
} catch (e: Throwable) {
    null
}
