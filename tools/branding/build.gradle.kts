// Regenerates every baked Phovo wordmark asset from the Fraunces source in docs/branding:
//     ./gradlew :tools:branding:run
// See docs/branding/README.md for why the wordmark is baked into art rather than drawn at runtime.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

// Skia's native library ships per host, so pick the runtime that matches the machine running the task.
val skikoHost: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osPart = when {
        os.contains("mac") -> "macos"
        os.contains("win") -> "windows"
        else -> "linux"
    }
    val archPart = if (arch == "aarch64" || arch == "arm64") "arm64" else "x64"
    "$osPart-$archPart"
}

dependencies {
    implementation(libs.skiko.awt)
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-$skikoHost:${libs.versions.skiko.get()}")
}

application {
    mainClass.set("com.serratocreations.phovo.branding.RenderBrandingKt")
}

tasks.named<JavaExec>("run") {
    args(rootProject.layout.projectDirectory.asFile.absolutePath)
}
