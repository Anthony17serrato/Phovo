package com.serratocreations.phovo.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.kotlin.dsl.register

/**
 * Registers `packageReleaseDistributions`, which builds the desktop formats published for the
 * machine it runs on and collects them in `build/release/`, named
 * `Phovo-<version>-<os>-<arch>.<ext>`. jpackage can only package for the OS and CPU it runs on, so
 * the release workflow runs this once per runner:
 *
 * - macOS (arm64, x64): DMG
 * - Windows (x64): MSI
 * - Linux x64: DEB, RPM and a portable tarball
 * - Linux arm64 (64-bit Raspberry Pi OS): DEB and a portable tarball
 *
 * Also registers `packageTarball`, a `.tar.gz` of the app folder from `createDistributable`, for
 * distros that take neither DEB nor RPM.
 */
fun Project.registerDesktopReleasePackaging() {
    val version = phovoVersion()
    val platform = hostPlatform()
    val arch = hostArch()
    val baseName = "Phovo-${version.name}-$platform-$arch"
    // The package tasks name their output after the numeric version, e.g. Phovo-1.0.11.dmg or
    // phovo_1.0.11_amd64.deb. Matching on it skips files left over from earlier versions.
    val packageVersion = version.desktopPackageVersion

    val tarball = tasks.register<Tar>("packageTarball") {
        group = "compose desktop"
        description = "Packages the app folder from createDistributable as a portable .tar.gz."
        dependsOn("createDistributable")
        from(layout.buildDirectory.dir("compose/binaries/main/app"))
        compression = Compression.GZIP
        archiveFileName.set("$baseName.tar.gz")
        destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/tarball"))
    }

    tasks.register<Sync>("packageReleaseDistributions") {
        group = "compose desktop"
        description = "Builds this machine's release formats into build/release as Phovo-<version>-<os>-<arch>.<ext>."
        into(layout.buildDirectory.dir("release"))
        // Two files mapping to the same release name means the match above is wrong: fail loudly.
        duplicatesStrategy = DuplicatesStrategy.FAIL

        fun collect(taskName: String, extension: String) {
            from(tasks.named(taskName)) {
                include("*$packageVersion*.$extension")
                rename { "$baseName.$extension" }
            }
        }
        when (platform) {
            "macos" -> collect("packageDmg", "dmg")
            "windows" -> collect("packageMsi", "msi")
            "linux" -> {
                collect("packageDeb", "deb")
                // RPM distros on ARM are rare, so arm64 ships DEB and the tarball only.
                if (arch == "x64") collect("packageRpm", "rpm")
                from(tarball)
            }
        }
    }
}

private fun hostPlatform(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> "macos"
        os.contains("win") -> "windows"
        os.contains("linux") -> "linux"
        else -> throw GradleException("No desktop release format for OS \"$os\".")
    }
}

private fun hostArch(): String = when (val arch = System.getProperty("os.arch").lowercase()) {
    "aarch64", "arm64" -> "arm64"
    "amd64", "x86_64" -> "x64"
    else -> throw GradleException("No desktop release format for CPU architecture \"$arch\".")
}
