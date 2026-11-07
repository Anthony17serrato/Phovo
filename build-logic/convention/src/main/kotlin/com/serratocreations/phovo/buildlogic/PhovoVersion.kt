package com.serratocreations.phovo.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.util.Properties

/**
 * The app version from `version.properties`, plus the numeric versions calculated from it.
 *
 * Installers can't use a SemVer pre-release version: macOS and Windows packages only take plain
 * numbers, Android's versionCode is an integer, and all of them refuse to install a lower number
 * over a higher one. So both numbers are calculated from the SemVer version, in a way that sorts
 * the same as SemVer across every release line. A stable patch (1.0.1) always sorts below a newer
 * beta (1.1.0-beta.2), so beta testers are never offered it as an upgrade.
 *
 * ```
 * versionCode = major × 1,000,000 + minor × 10,000 + patch × 100 + stage
 * stage:        alpha.N = 10 + N    beta.N = 50 + N    rc.N = 80 + N    stable = 99
 * ```
 *
 * The desktop package version is `major.minor.(patch × 100 + stage)`. "Versioning" in the README
 * has examples.
 */
data class PhovoVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val stage: Stage,
    /** The alpha, beta or rc number. 0 for a stable version. */
    val stageNumber: Int,
) {
    enum class Stage(val label: String, val base: Int, val maxNumber: Int) {
        // rc stops at 18: rc.19 would be 99, the same as the stable release.
        Alpha("alpha", base = 10, maxNumber = 39),
        Beta("beta", base = 50, maxNumber = 29),
        Rc("rc", base = 80, maxNumber = 18),
        Stable("", base = 99, maxNumber = 0),
    }

    val name: String =
        if (stage == Stage.Stable) "$major.$minor.$patch"
        else "$major.$minor.$patch-${stage.label}.$stageNumber"

    val isPrerelease: Boolean get() = stage != Stage.Stable

    private val stageCode: Int = if (stage == Stage.Stable) stage.base else stage.base + stageNumber

    val versionCode: Int = major * 1_000_000 + minor * 10_000 + patch * 100 + stageCode

    val desktopPackageVersion: String = "$major.$minor.${patch * 100 + stageCode}"

    companion object {
        private val pattern = Regex("""^(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta|rc)\.(\d+))?$""")

        fun parse(versionName: String): PhovoVersion {
            val match = pattern.matchEntire(versionName.trim()) ?: throw GradleException(
                "versionName \"$versionName\" isn't a supported version. Use MAJOR.MINOR.PATCH, " +
                    "optionally followed by -alpha.N, -beta.N or -rc.N (for example 1.0.0-alpha.1)."
            )
            val (major, minor, patch) = match.groupValues.subList(1, 4).map { it.toInt() }
            val stage = Stage.entries.firstOrNull { it.label == match.groupValues[4] && it != Stage.Stable }
                ?: Stage.Stable
            val stageNumber = match.groupValues[5].toIntOrNull() ?: 0

            fun fail(reason: String): Nothing = throw GradleException(
                "versionName \"$versionName\" can't be encoded: $reason. See \"Versioning\" in the README."
            )
            // macOS needs a major above 0 and Windows Installer caps it at 255.
            if (major !in 1..255) fail("major must be 1 to 255")
            if (minor !in 0..99) fail("minor must be 0 to 99")
            if (patch !in 0..99) fail("patch must be 0 to 99")
            if (stage != Stage.Stable && stageNumber !in 1..stage.maxNumber) {
                fail("${stage.label} must be numbered 1 to ${stage.maxNumber}")
            }
            return PhovoVersion(major, minor, patch, stage, stageNumber)
        }
    }
}

/** Reads `version.properties` at the root of the build. */
fun Project.phovoVersion(): PhovoVersion {
    val file = rootProject.layout.projectDirectory.file("version.properties")
    val text = providers.fileContents(file).asText.orNull
        ?: throw GradleException("Missing ${file.asFile}, which holds the app's versionName.")
    val versionName = Properties().apply { load(text.reader()) }.getProperty("versionName")
        ?: throw GradleException("${file.asFile} has no versionName entry.")
    return PhovoVersion.parse(versionName)
}
