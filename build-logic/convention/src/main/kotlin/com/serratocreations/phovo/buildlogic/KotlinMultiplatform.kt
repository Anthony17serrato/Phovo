package com.serratocreations.phovo.buildlogic

import com.android.build.api.dsl.*
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import java.net.URI

@OptIn(KotlinNativeCacheApi::class)
internal fun Project.configureKotlinMultiplatform(
    /**
     * To learn more about umbrella framework see:
     * https://kotlinlang.org/docs/multiplatform/multiplatform-project-configuration.html#several-shared-modules
     */
    isUmbrella: Boolean,
    isApplication: Boolean = false,
    customSourceSets: Set<CustomSourceSets> = emptySet(),
    // All targets are configured by default
    targetList: Set<Targets> = Targets.entries.toSet()
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
            jvm()
        }

        if (targetList.contains(Targets.ANDROID) && isApplication.not()) {
            configure<KotlinMultiplatformAndroidLibraryTarget> {
                // TODO: Investigate if these can be pulled from TOML file
                compileSdk = 37
                minSdk = 23
                androidResources.enable = true
                withHostTestBuilder {}.configure {}
                withDeviceTestBuilder {
                    sourceSetTreeName = "test"
                }
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }

        if (targetList.contains(Targets.WEB)) {
            js {
                browser()
                if (isApplication) {
                    outputModuleName.set("composeApp")
                    binaries.executable()
                }
            }

            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
                if (isApplication) {
                    outputModuleName.set("composeApp")
                    binaries.executable()
                }
            }
        }

        if (isUmbrella && targetList.contains(Targets.IOS)) {
            listOf(
                iosArm64(),
                iosSimulatorArm64()
            ).forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = "ComposeApp"
                    isStatic = true

                    // Disable compilation cache for this binary
                    disableNativeCache(
                        version = DisableCacheInKotlinVersion.`2_4_10`,
                        reason = "Material icons extended bug, this library is deprecated and no longer Maintained, refactor the code to remove this library then remove this piece of code",
                        issueUrl = URI("https://slack-chats.kotlinlang.org/t/27579160/after-updating-to-1-8-0-rc01-package-androidx-compose-materi")
                    )
                }
            }
        } else if (targetList.contains(Targets.IOS)) {
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
                    sourceSets.jvmMain.get().dependsOn(commonDesktopIosAndroid)
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
                    sourceSets.webMain.get().dependsOn(commonAndroidIosWeb)
                    // Ensure commonIosAndroid depends on commonAndroidIosWeb
                    sourceSets.named(CustomSourceSets.IosAndroid.sourceSetName).get().dependsOn(commonAndroidIosWeb)
                }
                
                CustomSourceSets.AndroidDesktop -> {
                    val commonAndroidDesktop = sourceSets.create(sourceSet.sourceSetName) {
                        dependsOn(sourceSets.commonMain.get())
                        dependencies {

                        }
                    }
                    sourceSets.jvmMain.get().dependsOn(commonAndroidDesktop)
                    sourceSets.androidMain.get().dependsOn(commonAndroidDesktop)
                }
            }
        }
    }
}