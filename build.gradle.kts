plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.room) apply false
    id(libs.plugins.phovo.kmp.library.library.get().pluginId) apply false
    id(libs.plugins.phovo.kmp.application.application.get().pluginId) apply false
    id(libs.plugins.phovo.kmp.application.koin.get().pluginId) apply false
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId) apply false
    id(libs.plugins.phovo.kmp.library.compose.get().pluginId) apply false
    id(libs.plugins.phovo.kmp.application.compose.get().pluginId) apply false
    id(libs.plugins.phovo.kmp.library.room.get().pluginId) apply false
}