import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.kotlin.dsl.sourceSets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

compose.resources {
    publicResClass = true
    generateResClass = always
    customDirectory(
        sourceSetName = "jvmMain",
        directoryProvider = tasks.register<CopyPlatformFFmpeg>("copyPlatformFFmpeg") {
            val osName = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()
            val isArm = arch.contains("aarch64") || arch.contains("arm")

            // For binary updates check https://ffmpeg.org/download.html
            val ffmpegSourceFile = when {
                osName.contains("mac") && isArm -> project.file("ffmpeg-binaries/mac-arm/ffmpeg")
                osName.contains("mac") -> project.file("ffmpeg-binaries/mac-x64/ffmpeg")
                // Win32 support is possible by manually building the binary https://github.com/BtbN/FFmpeg-Builds/tree/latest
                osName.contains("win") && isArm -> project.file("ffmpeg-binaries/win-arm/ffmpeg.exe")
                osName.contains("win") -> project.file("ffmpeg-binaries/win-x64/ffmpeg.exe")

                osName.contains("linux") && isArm -> project.file("ffmpeg-binaries/linux-arm/ffmpeg")
                osName.contains("linux") -> project.file("ffmpeg-binaries/linux-x64/ffmpeg")

                else -> error("Unsupported OS: $osName $arch")
            }
            ffmpegSource.set(ffmpegSourceFile)
            outputDir.set(project.layout.buildDirectory.dir("generatedFfmpegResources"))
        }.flatMap { it.outputDir }
    )
}

abstract class CopyPlatformFFmpeg : DefaultTask() {

    @get:InputFile
    abstract val ffmpegSource: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val sourceFile = ffmpegSource.get().asFile
        val targetDir = outputDir.get().asFile.resolve("files")
        targetDir.mkdirs()
        Files.copy(sourceFile.toPath(), targetDir.resolve(sourceFile.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
        println("Copied FFmpeg to $targetDir")
    }
}