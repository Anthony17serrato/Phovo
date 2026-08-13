import com.serratocreations.phovo.buildlogic.ffmpeg.FfmpegBinaries
import com.serratocreations.phovo.buildlogic.ffmpeg.ProvisionFfmpegTask
import org.gradle.kotlin.dsl.sourceSets

plugins {
    alias(libs.plugins.phovo.kmp.desktop.library)
    alias(libs.plugins.phovo.kmp.library.compose)
    alias(libs.plugins.phovo.kmp.library.koin)
}

kotlin {
    sourceSets {
        jvmMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.resources)
            implementation(libs.filekit.core)
            implementation(projects.core.common)
            implementation(projects.core.logger)
        }
    }
}

// FFmpeg is downloaded and checksum verified at build time rather than committed through Git LFS,
// which has quota limits. See FfmpegBinaries for the pinned builds and how to update them.
compose.resources {
    publicResClass = true
    generateResClass = always
    customDirectory(
        sourceSetName = "jvmMain",
        directoryProvider = tasks.register<ProvisionFfmpegTask>("provisionFfmpeg") {
            val distribution = FfmpegBinaries.forHost()

            platform.set(distribution.platform)
            downloadUrl.set(distribution.url)
            sha256.set(distribution.sha256)
            archiveFormat.set(distribution.format)
            executableName.set(distribution.executableName)

            // Outside the build directory so `clean` does not force another download.
            archiveCacheDirectory.set(gradle.gradleUserHomeDir.resolve("caches/phovo-ffmpeg"))
            offline.set(gradle.startParameter.isOffline)

            outputDirectory.set(project.layout.buildDirectory.dir("generatedFfmpegResources"))
        }.flatMap { it.outputDirectory }
    )
}
