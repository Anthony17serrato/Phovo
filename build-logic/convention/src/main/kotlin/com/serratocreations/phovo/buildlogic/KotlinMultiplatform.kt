package com.serratocreations.phovo.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configure base Kotlin Multiplatform with Android options
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = 36

        defaultConfig {
            minSdk = 23
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
    customSourceSets: Set<CustomSourceSets> = emptySet(),
    // All targets are configured by default
    targetList: Set<Targets> = Targets.values().toSet()
) {
    configure<KotlinMultiplatformExtension> {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        if (customSourceSets.isNotEmpty()) {
            // Apply the default hierarchy again. It'll create, for example, the iosMain source set:
            // https://kotlinlang.org/docs/multiplatform-hierarchy.html
            applyDefaultHierarchyTemplate()
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
                browser()
                if (isApplication) {
                    outputModuleName.set("composeApp")
                    binaries.executable()
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

        customSourceSets.sortedBy { it.declarationOrder }.forEach { sourceSet ->
            when (sourceSet) {
                CustomSourceSets.DesktopIosAndroid -> {
                    val commonDesktopIosAndroid = sourceSets.create(sourceSet.sourceSetName) {
                        dependsOn(sourceSets.commonMain.get())
                        dependencies {

                        }
                    }
                    sourceSets.iosMain.get().dependsOn(commonDesktopIosAndroid)
                    sourceSets.androidMain.get().dependsOn(commonDesktopIosAndroid)
                    sourceSets.named("desktopMain").get().dependsOn(commonDesktopIosAndroid)
                    // Ensure commonIosAndroid depends on commonDesktopIosAndroid
                    sourceSets.named(CustomSourceSets.IosAndroid.sourceSetName).get().dependsOn(commonDesktopIosAndroid)
                    sourceSets.named(CustomSourceSets.AndroidDesktop.sourceSetName).get().dependsOn(commonDesktopIosAndroid)
                }

                CustomSourceSets.IosAndroid -> {
                    val commonIosAndroid = sourceSets.create(sourceSet.sourceSetName) {
                        dependsOn(sourceSets.commonMain.get())
                        dependencies {

                        }
                    }
                    sourceSets.iosMain.get().dependsOn(commonIosAndroid)
                    sourceSets.androidMain.get().dependsOn(commonIosAndroid)
                }

                CustomSourceSets.AndroidIosWeb -> {
                    val commonAndroidIosWeb = sourceSets.create(sourceSet.sourceSetName) {
                        dependsOn(sourceSets.commonMain.get())
                        dependencies {

                        }
                    }
                    sourceSets.iosMain.get().dependsOn(commonAndroidIosWeb)
                    sourceSets.androidMain.get().dependsOn(commonAndroidIosWeb)
                    sourceSets.wasmJsMain.get().dependsOn(commonAndroidIosWeb)
                    // Ensure commonIosAndroid depends on commonAndroidIosWeb
                    sourceSets.named(CustomSourceSets.IosAndroid.sourceSetName).get().dependsOn(commonAndroidIosWeb)
                }
                
                CustomSourceSets.AndroidDesktop -> {
                    val commonAndroidDesktop = sourceSets.create(sourceSet.sourceSetName) {
                        dependsOn(sourceSets.commonMain.get())
                        dependencies {

                        }
                    }
                    sourceSets.named("desktopMain").get().dependsOn(commonAndroidDesktop)
                    sourceSets.androidMain.get().dependsOn(commonAndroidDesktop)
                }
            }
        }
    }
}