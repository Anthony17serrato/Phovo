package com.serratocreations.phovo.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

/**
 * Configure base Kotlin Multiplatform with Android options
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = 35

        defaultConfig {
            minSdk = 21
        }

        compileOptions {
            // Up to Java 11 APIs are available through desugaring
            // https://developer.android.com/studio/write/java11-minimal-support-table
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
            //isCoreLibraryDesugaringEnabled = true
        }
    }
}

internal fun Project.configureKotlinMultiplatform(
    isApplication: Boolean,
    targetList: List<Targets> = Targets.values().asList()
) {
    configure<KotlinMultiplatformExtension> {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        if (targetList.contains(Targets.DESKTOP)) {
            jvm("desktop")
        }

        if (targetList.contains(Targets.ANDROID)) {
            androidTarget {
                compilations.all {
                    compileTaskProvider.configure {
                        compilerOptions {
                            jvmTarget.set(JvmTarget.JVM_11)
                        }
                    }
                }
            }
        }

        if (targetList.contains(Targets.WASM)) {
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                if (isApplication) {
                    outputModuleName.set("composeApp")
                    browser {
                        commonWebpackConfig {
                            outputFileName = "composeApp.js"
                            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                                static = (static ?: mutableListOf()).apply {
                                    // Serve sources to debug inside browser
                                    add(project.rootDir.path)
                                    add(project.projectDir.path)
                                }
                            }
                        }
                    }
                    binaries.executable()
                } else {
                    browser()
                }
            }
        }

        if (isApplication && targetList.contains(Targets.IOS)) {
            listOf(
                iosX64(),
                iosArm64(),
                iosSimulatorArm64()
            ).forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = "ComposeApp"
                    isStatic = true
                }
            }
        } else if (targetList.contains(Targets.IOS)) {
            iosX64()
            iosArm64()
            iosSimulatorArm64()
        }
    }
}