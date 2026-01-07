package com.serratocreations.phovo.buildlogic

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configure Koin-specific options
 */
internal fun Project.configureKmpKoin() {
    // Get the list of configured targets by examining the source sets
    val configuredTargets = mutableSetOf<Targets>()

    extensions.configure<KotlinMultiplatformExtension> {
        val sourceSetNames = sourceSets.names

        // Check for iOS targets
        if (sourceSetNames.any { it.startsWith("ios")}) {
            configuredTargets.add(Targets.IOS)
        }

        // Check for Desktop target
        if (sourceSetNames.any { it.contains("desktopMain")}) {
            configuredTargets.add(Targets.DESKTOP)
        }

        // Check for WASM target
        if (sourceSetNames.any { it.contains("wasmJsMain")}) {
            configuredTargets.add(Targets.WASM)
        }
        // Check for Android target
        if (sourceSetNames.any { it.contains("androidMain")}) {
            configuredTargets.add(Targets.ANDROID)
            sourceSets.androidMain.dependencies {
                implementation(libs.findLibrary("koin.android").get())
            }
        }

        sourceSets.commonMain.dependencies {
            // Koin
            implementation(libs.findBundle("koin.common.kmp").get())
            // TODO Koin Annotations is not stable for KMP(Leaving configuration for reference purposes)
            // api(libs.findLibrary("koin.annotations").get())
        }
    }

   this.apply {
        dependencies {
//            // Common KSP compiler is always needed
//            add("kspCommonMainMetadata", libs.findLibrary("koin.ksp.compiler").get())
//
//            // Add target-specific KSP compilers only for configured targets
//            if (configuredTargets.contains(Targets.ANDROID)) {
//                add("kspAndroid", libs.findLibrary("koin.ksp.compiler").get())
//            }
//
//            if (configuredTargets.contains(Targets.WASM)) {
//                add("kspWasmJs", libs.findLibrary("koin.ksp.compiler").get())
//            }
//
//            if (configuredTargets.contains(Targets.DESKTOP)) {
//                add("kspDesktop", libs.findLibrary("koin.ksp.compiler").get())
//            }
//
//            if (configuredTargets.contains(Targets.IOS)) {
//                add("kspIosX64", libs.findLibrary("koin.ksp.compiler").get())
//                add("kspIosArm64", libs.findLibrary("koin.ksp.compiler").get())
//                add("kspIosSimulatorArm64", libs.findLibrary("koin.ksp.compiler").get())
//            }
        }
    }

//    // Trigger Common Metadata Generation from Native tasks
//    project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
//        if (name != "kspCommonMainKotlinMetadata") {
//            dependsOn("kspCommonMainKotlinMetadata")
//        }
//    }
//
//    project.tasks.withType(KspAATask::class.java).configureEach {
//        if (name != "kspCommonMainKotlinMetadata") {
//            dependsOn("kspCommonMainKotlinMetadata")
//        }
//    }

    extensions.configure<KspExtension> {
        // Enable Koin Viewmodel Annotation
        arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
        arg("KOIN_CONFIG_CHECK", "true")
        arg("KOIN_LOG_TIMES", "true")
    }
}