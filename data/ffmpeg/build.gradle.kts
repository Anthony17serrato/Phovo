import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id(libs.plugins.phovo.kmp.desktop.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.compose.get().pluginId)
}

kotlin {
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.compose.resources)
            }
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
    customDirectory(
        sourceSetName = "desktopMain",
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
        println("Copied FFmpeg for $osName ($arch) to $targetDir")
    }
}