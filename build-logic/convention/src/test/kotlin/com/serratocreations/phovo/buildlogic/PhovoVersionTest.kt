package com.serratocreations.phovo.buildlogic

import org.gradle.api.GradleException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks in the version scheme. Android and Windows Installer refuse to install a lower number over
 * a higher one, so a change to the formula that breaks ordering would strand existing installs.
 */
class PhovoVersionTest {

    @Test
    fun `matches the README table`() {
        val expected = mapOf(
            "1.0.0-alpha.1" to (1000011 to "1.0.11"),
            "1.0.0" to (1000099 to "1.0.99"),
            "1.0.1" to (1000199 to "1.0.199"),
            "1.1.0-beta.2" to (1010052 to "1.1.52"),
            "1.1.0" to (1010099 to "1.1.99"),
        )
        expected.forEach { (name, numbers) ->
            val version = PhovoVersion.parse(name)
            assertEquals(numbers.first, version.versionCode, "versionCode of $name")
            assertEquals(numbers.second, version.desktopPackageVersion, "desktop version of $name")
            assertEquals(name, version.name)
        }
    }

    @Test
    fun `numbers follow SemVer order across release lines`() {
        // Ascending SemVer order, mixing stable patches with a newer beta line.
        val ascending = listOf(
            "1.0.0-alpha.1", "1.0.0-alpha.9", "1.0.0-alpha.10", "1.0.0-alpha.39",
            "1.0.0-beta.1", "1.0.0-beta.29",
            "1.0.0-rc.1", "1.0.0-rc.18",
            "1.0.0", "1.0.1", "1.0.99",
            "1.1.0-alpha.1", "1.1.0-beta.2", "1.1.0", "1.1.1",
            "1.99.99", "2.0.0-alpha.1", "2.0.0", "255.99.99",
        ).map(PhovoVersion::parse)

        ascending.zipWithNext().forEach { (lower, higher) ->
            assertTrue(
                lower.versionCode < higher.versionCode,
                "versionCode ${lower.name} (${lower.versionCode}) must be below ${higher.name} (${higher.versionCode})"
            )
            assertTrue(
                compareNumeric(lower.desktopPackageVersion, higher.desktopPackageVersion) < 0,
                "desktop ${lower.desktopPackageVersion} (${lower.name}) must be below ${higher.desktopPackageVersion} (${higher.name})"
            )
        }
    }

    @Test
    fun `numbers stay inside platform limits`() {
        val highest = PhovoVersion.parse("255.99.99")
        // Google Play's maximum versionCode.
        assertTrue(highest.versionCode <= 2_100_000_000)
        // Windows Installer: MAJOR and MINOR up to 255, BUILD up to 65535.
        val (major, minor, build) = highest.desktopPackageVersion.split('.').map(String::toInt)
        assertTrue(major <= 255 && minor <= 255 && build <= 65535)
    }

    @Test
    fun `accepts the last number of each stage`() {
        listOf("1.0.0-alpha.39", "1.0.0-beta.29", "1.0.0-rc.18", "1.99.99", "255.0.0").forEach {
            PhovoVersion.parse(it)
        }
    }

    @Test
    fun `rejects versions the scheme can't encode`() {
        listOf(
            "1.0.0-alpha.0", "1.0.0-alpha.40", "1.0.0-beta.30",
            // rc.19 would encode to 99, the same as the stable release.
            "1.0.0-rc.19",
            // macOS needs a major above 0; Windows Installer caps it at 255.
            "0.1.0", "256.0.0",
            "1.100.0", "1.0.100",
        ).forEach { name ->
            assertFailsWith<GradleException>(name) { PhovoVersion.parse(name) }
        }
    }

    @Test
    fun `rejects malformed versions`() {
        listOf("1.0", "1.0.0-alpha10", "1.0.0-alpha", "1.0.0-preview.1", "v1.0.0", "1.0.0.1", "").forEach { name ->
            assertFailsWith<GradleException>(name) { PhovoVersion.parse(name) }
        }
    }

    @Test
    fun `flags pre-releases`() {
        assertTrue(PhovoVersion.parse("1.0.0-beta.1").isPrerelease)
        assertFalse(PhovoVersion.parse("1.0.0").isPrerelease)
    }

    private fun compareNumeric(a: String, b: String): Int {
        val left = a.split('.').map(String::toInt)
        val right = b.split('.').map(String::toInt)
        return left.zip(right).map { (l, r) -> l.compareTo(r) }.firstOrNull { it != 0 } ?: 0
    }
}
