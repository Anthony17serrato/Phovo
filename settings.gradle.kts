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
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
    }
}

include(":composeApp")
// Core
include(":core:designsystem")
include(":core:common")
include(":core:logger")

// Features
include(":feature:photos")
include(":feature:connections")

// Data
include(":data:photos")
include(":data:server")