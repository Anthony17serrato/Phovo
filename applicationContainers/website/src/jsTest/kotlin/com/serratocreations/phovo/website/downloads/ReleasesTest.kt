package com.serratocreations.phovo.website.downloads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The download buttons depend on matching release file names to devices. If release.yml or the
 * desktop packaging renames its files, these fail instead of every button quietly falling back to
 * the releases page.
 */
class ReleasesTest {
    private val base = "https://github.com/Anthony17serrato/Phovo/releases/download/v1.0.0-alpha.1"

    private fun asset(name: String, size: Long = 1_048_576) =
        """{"name":"$name","size":$size,"browser_download_url":"$base/$name"}"""

    private fun release(tag: String, assets: List<String>, draft: Boolean = false, prerelease: Boolean = true) =
        """{"tag_name":"$tag","html_url":"https://github.com/Anthony17serrato/Phovo/releases/tag/$tag",
           "draft":$draft,"prerelease":$prerelease,"assets":[${assets.joinToString(",")}]}"""

    // Every file name release.yml publishes for 1.0.0-alpha.1.
    private val publishedNames = listOf(
        "Phovo-1.0.0-alpha.1-android.apk" to Target.ANDROID,
        "Phovo-1.0.0-alpha.1-linux-arm64.deb" to Target.PI_DEB,
        "Phovo-1.0.0-alpha.1-linux-arm64.tar.gz" to Target.PI_TAR,
        "Phovo-1.0.0-alpha.1-linux-x64.deb" to Target.LINUX_DEB,
        "Phovo-1.0.0-alpha.1-linux-x64.rpm" to Target.LINUX_RPM,
        "Phovo-1.0.0-alpha.1-linux-x64.tar.gz" to Target.LINUX_TAR,
        "Phovo-1.0.0-alpha.1-macos-arm64.dmg" to Target.MAC_ARM,
        "Phovo-1.0.0-alpha.1-macos-x64.dmg" to Target.MAC_INTEL,
        "Phovo-1.0.0-alpha.1-windows-x64.msi" to Target.WINDOWS,
    )

    @Test
    fun everyPublishedFileMatchesItsTarget() {
        publishedNames.forEach { (name, target) -> assertEquals(target, targetForFileName(name), name) }
    }

    @Test
    fun everyTargetHasAPublishedFile() {
        assertEquals(Target.entries.toSet(), publishedNames.map { it.second }.toSet())
    }

    @Test
    fun unrelatedFilesMatchNothing() {
        listOf("SHA256SUMS", "Phovo-1.0.0-windows-arm64.msi", "Phovo-1.0.0-ios.ipa", "notes.txt")
            .forEach { assertNull(targetForFileName(it), it) }
    }

    @Test
    fun parsesTheNewestReleaseWithFiles() {
        val json = "[" + listOf(
            release("v1.0.0-alpha.2", emptyList(), draft = true),
            release("v1.0.0-alpha.1", publishedNames.map { asset(it.first) } + asset("SHA256SUMS", 912)),
            release("v0.9.0", listOf(asset("Phovo-0.9.0-android.apk"))),
        ).joinToString(",") + "]"

        val parsed = parseReleases(json)

        requireNotNull(parsed) { "expected a release" }
        assertEquals("1.0.0-alpha.1", parsed.version)
        assertTrue(parsed.isPrerelease)
        assertEquals(Target.entries.size, parsed.files.size)
        assertEquals("$base/Phovo-1.0.0-alpha.1-macos-arm64.dmg", parsed.file(Target.MAC_ARM)?.url)
        assertEquals("1 MB", parsed.file(Target.ANDROID)?.sizeLabel)
        assertEquals("$base/SHA256SUMS", parsed.checksumsUrl)
    }

    @Test
    fun skipsReleasesWithoutFiles() {
        val json = "[" + release("v1.0.0-alpha.2", emptyList()) + "," +
            release("v1.0.0-alpha.1", listOf(asset("Phovo-1.0.0-alpha.1-android.apk"))) + "]"
        assertEquals("1.0.0-alpha.1", parseReleases(json)?.version)
    }

    @Test
    fun returnsNullForGithubErrorsAndEmptyLists() {
        assertNull(parseReleases("""{"message":"API rate limit exceeded","documentation_url":"https://docs.github.com"}"""))
        assertNull(parseReleases("[]"))
    }

    @Test
    fun skipsAssetsWithMissingFields() {
        val broken = """{"name":"Phovo-1.0.0-alpha.1-macos-x64.dmg","browser_download_url":"$base/x.dmg"}"""
        val json = "[" + release("v1.0.0-alpha.1", listOf(broken, asset("Phovo-1.0.0-alpha.1-android.apk"))) + "]"
        val parsed = parseReleases(json)
        assertNull(parsed?.file(Target.MAC_INTEL))
        assertFalse(parsed?.file(Target.ANDROID) == null)
    }

    @Test
    fun stageLabels() {
        fun label(version: String) = Release(version, "", isPrerelease = true, files = emptyList(), checksumsUrl = null).stageLabel
        assertEquals("Alpha 1", label("1.0.0-alpha.1"))
        assertEquals("Beta 12", label("1.1.0-beta.12"))
        assertEquals("Release candidate 2", label("1.0.0-rc.2"))
        assertEquals("1.0.0", label("1.0.0"))
    }
}
