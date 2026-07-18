plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library) apply false
    alias(libs.plugins.phovo.kmp.android.ios.desktop.library) apply false
    alias(libs.plugins.phovo.kmp.android.ios.library) apply false
    alias(libs.plugins.phovo.kmp.umbrella.library) apply false
    alias(libs.plugins.phovo.kmp.library.koin) apply false
    alias(libs.plugins.phovo.kmp.library.compose) apply false
    alias(libs.plugins.phovo.kmp.library.room) apply false
    alias(libs.plugins.phovo.kmp.desktop.library) apply false
    alias(libs.plugins.phovo.kmp.desktop.application) apply false
    alias(libs.plugins.phovo.kmp.android.application) apply false
}
