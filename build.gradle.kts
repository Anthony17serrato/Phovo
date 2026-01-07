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
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library) apply false
    alias(libs.plugins.phovo.kmp.android.ios.desktop.library) apply false
    alias(libs.plugins.phovo.kmp.application.application) apply false
    alias(libs.plugins.phovo.kmp.application.koin) apply false
    alias(libs.plugins.phovo.kmp.library.koin) apply false
    alias(libs.plugins.phovo.kmp.library.compose) apply false
    alias(libs.plugins.phovo.kmp.application.compose) apply false
    alias(libs.plugins.phovo.kmp.library.room) apply false
    alias(libs.plugins.phovo.kmp.desktop.library) apply false
}