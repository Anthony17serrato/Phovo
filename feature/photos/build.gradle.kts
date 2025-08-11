import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id(libs.plugins.phovo.kmp.android.ios.desktop.web.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId)
    id(libs.plugins.phovo.kmp.library.compose.get().pluginId)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.coil.video)
                implementation(libs.exoplayer)
                implementation(libs.media.ui)
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(projects.core.designsystem)
                implementation(projects.core.common)
                implementation(projects.core.logger)
                implementation(projects.data.photos)
                implementation(projects.data.server)

                implementation(compose.components.resources)
                implementation(libs.serialization.json)
                implementation(libs.coil.compose)
                implementation(libs.kotlin.datetime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        iosMain {
            dependencies {
                implementation(libs.skiko)
            }
        }
        val desktopMain by getting {
            dependencies {
            }
        }
    }
}

android {
    namespace = "com.serratocreations.phovo.feature.photos"
}

compose.resources {
    customDirectory(
        sourceSetName = "desktopMain", // or jvmMain depending on your setup
        directoryProvider = tasks.register<CopyPlatformFFmpeg>("copyPlatformFFmpeg").map { it.outputDir.get() }
    )
}

abstract class CopyPlatformFFmpeg : DefaultTask() {

    @get:OutputDirectory
    val outputDir = project.layout.buildDirectory.dir("generatedFfmpegResources")

    @TaskAction
    fun run() {
        val osName = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        val isArm = arch.contains("aarch64") || arch.contains("arm")

        // For binary updates check https://ffmpeg.org/download.html
        val ffmpegSource = when {
            osName.contains("mac") && isArm -> project.file("ffmpeg-binaries/mac-arm/ffmpeg")
            osName.contains("mac") -> project.file("ffmpeg-binaries/mac-x64/ffmpeg")
            // Win32 support is possible by manually building the binary https://github.com/BtbN/FFmpeg-Builds/tree/latest
            osName.contains("win") && isArm -> project.file("ffmpeg-binaries/win-arm/ffmpeg.exe")
            osName.contains("win") -> project.file("ffmpeg-binaries/win-x64/ffmpeg.exe")

            osName.contains("linux") && isArm -> project.file("ffmpeg-binaries/linux-arm/ffmpeg")
            osName.contains("linux") -> project.file("ffmpeg-binaries/linux-x64/ffmpeg")

            else -> error("Unsupported OS: $osName $arch")
        }

        val targetDir = outputDir.get().asFile.resolve("files")
        targetDir.mkdirs()
        Files.copy(ffmpegSource.toPath(), targetDir.resolve(ffmpegSource.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
        println("✅ Copied FFmpeg for $osName ($arch) to $targetDir")
    }
}
