rootProject.name = "Phovo"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
    }
}

include(":phovoApp")
// Core
include(":core:designsystem")
include(":core:common")
include(":core:logger")
include(":core:database")
include(":core:model")

// Features
include(":feature:photos")
include(":feature:connections")

// Data
include(":data:photos")
include(":data:server")
include(":data:ffmpeg")