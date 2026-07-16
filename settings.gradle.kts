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

// Application containers
include(":applicationContainers:desktopApp")
include(":applicationContainers:androidApp")

// Umbrella module based on https://kotlinlang.org/docs/multiplatform/multiplatform-project-configuration.html#several-shared-modules
include(":sharedumbrella")

// Core
include(":core:designsystem")
include(":core:common")
include(":core:logger")
include(":core:database")
include(":core:model")
include(":core:navigation")
include(":core:domain")
include(":core:serverconfig")

// Features
include(":feature:photos")
include(":feature:connections:core")
include(":feature:connections:client")

// Data
include(":data:photos")
include(":data:server")
include(":data:thumbnails")