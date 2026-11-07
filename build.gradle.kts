import com.serratocreations.phovo.buildlogic.getBuildFlavor
import com.serratocreations.phovo.buildlogic.phovoVersion

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

// Prints the app version for the release workflow, so the versionCode formula lives in one place
// (build-logic's PhovoVersion.kt). Run with -q to get just the key=value lines.
tasks.register("printVersion") {
    val version = phovoVersion()
    val flavor = getBuildFlavor().flavorName
    doLast {
        println("versionName=${version.name}")
        println("versionCode=${version.versionCode}")
        println("desktopPackageVersion=${version.desktopPackageVersion}")
        println("prerelease=${version.isPrerelease}")
        println("flavor=$flavor")
    }
}
