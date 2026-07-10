package com.serratocreations.phovo.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion

/**
 * Configure base Kotlin Multiplatform with Android options
 */
internal fun ApplicationExtension.configureAndroidApplication() {
    namespace = "com.serratocreations.phovo"
    // TODO: Investigate if these can be pulled from TOML file
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        // Up to Java 11 APIs are available through desugaring
        // https://developer.android.com/studio/write/java11-minimal-support-table
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        //isCoreLibraryDesugaringEnabled = true
    }
}