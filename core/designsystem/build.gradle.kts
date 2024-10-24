import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

group = "io.github.kotlin"
version = "1.0.0"

kotlin {
    jvm("desktop")
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                api(compose.components.resources)
                api(compose.components.uiToolingPreview)
                api(compose.materialIconsExtended)
                api(compose.material)
                api(compose.foundation)
                api(compose.material3)
                //api(compose.material3AdaptiveNavigationSuite)
                api(compose.runtime)
                api(compose.ui)
                api(libs.compose.adaptive)
                api(libs.compose.adaptive.layout)
                api(libs.compose.adaptive.navigation)
                api(libs.compose.foundation.layout)
                api(libs.compose.material.navigation)
                api(libs.compose.navigation)
                api(libs.compose.navigation.common)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.serratocreations.phovo.core.designsystem"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
