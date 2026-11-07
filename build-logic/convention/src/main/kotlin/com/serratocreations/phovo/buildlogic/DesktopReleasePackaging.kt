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
    val osName = System.getProperty("os.name")
    val cpuArch = System.getProperty("os.arch")
    // Unknown hosts (a 32-bit JVM, riscv64, FreeBSD, ...) get null here instead of an exception, so
    // only running the release task fails on them, not configuring the whole build.
    val platform = hostPlatform(osName)
    val arch = hostArch(cpuArch)
    // The package tasks name their output after the numeric version, e.g. Phovo-1.0.11.dmg or
    // phovo_1.0.11_amd64.deb. Matching on it skips files left over from earlier versions.
    val packageVersion = version.desktopPackageVersion

    val tarball = tasks.register<Tar>("packageTarball") {
        group = "compose desktop"
        description = "Packages the app folder from createDistributable as a portable .tar.gz."
        dependsOn("createDistributable")
        from(layout.buildDirectory.dir("compose/binaries/main/app"))
        // Gradle 9 archives store every file as 0644 by default (reproducible builds), which leaves
        // bin/Phovo and the bundled JVM's executables unrunnable once unpacked. Keep their real modes.
        useFileSystemPermissions()
        compression = Compression.GZIP
        archiveFileName.set("Phovo-${version.name}-${platform ?: osName}-${arch ?: cpuArch}.tar.gz")
        destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/tarball"))
    }

    val releaseDescription =
        "Builds this machine's release formats into build/release as Phovo-<version>-<os>-<arch>.<ext>."
    if (platform == null || arch == null) {
        tasks.register("packageReleaseDistributions") {
            group = "compose desktop"
            description = releaseDescription
            doLast {
                throw GradleException(
                    "No desktop release format for $osName on $cpuArch. Releases are built on macOS " +
                        "(arm64, x64), Windows (x64) and Linux (x64, arm64)."
                )
            }
        }
        return
    }

    val baseName = "Phovo-${version.name}-$platform-$arch"
    // The DEB as jpackage builds it only installs on Debian 13 / Ubuntu 24.04+; see WidenDebDependenciesTask.
    val widenDeb = if (platform == "linux") {
        tasks.register<WidenDebDependenciesTask>("widenDebDependencies") {
            group = "compose desktop"
            description = "Rewrites the DEB so its t64 dependencies also accept their pre-t64 names."
            debDirectory.set(tasks.named("packageDeb").map { layout.buildDirectory.dir("compose/binaries/main/deb").get() })
            this.packageVersion.set(version.desktopPackageVersion)
            outputDirectory.set(layout.buildDirectory.dir("compose/binaries/main/deb-widened"))
        }
    } else {
        null
    }
    tasks.register<Sync>("packageReleaseDistributions") {
        group = "compose desktop"
        description = releaseDescription
        into(layout.buildDirectory.dir("release"))
        // Two files mapping to the same release name means the match above is wrong: fail loudly.
        duplicatesStrategy = DuplicatesStrategy.FAIL

        fun collect(source: Any, extension: String) {
            from(source) {
                include("*$packageVersion*.$extension")
                rename { "$baseName.$extension" }
            }
        }
        when (platform) {
            "macos" -> collect(tasks.named("packageDmg"), "dmg")
            "windows" -> collect(tasks.named("packageMsi"), "msi")
            "linux" -> {
                collect(widenDeb ?: tasks.named("packageDeb"), "deb")
                // RPM distros on ARM are rare, so arm64 ships DEB and the tarball only.
                if (arch == "x64") collect(tasks.named("packageRpm"), "rpm")
                from(tarball)
            }
        }
    }
}

private fun hostPlatform(osName: String): String? {
    val os = osName.lowercase()
    return when {
        os.contains("mac") -> "macos"
        os.contains("win") -> "windows"
        os.contains("linux") -> "linux"
        else -> null
    }
}

private fun hostArch(cpuArch: String): String? = when (cpuArch.lowercase()) {
    "aarch64", "arm64" -> "arm64"
    "amd64", "x86_64" -> "x64"
    else -> null
}
