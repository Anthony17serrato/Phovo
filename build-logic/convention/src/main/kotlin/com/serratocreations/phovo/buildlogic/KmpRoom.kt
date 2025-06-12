package com.serratocreations.phovo.buildlogic

import androidx.room.gradle.RoomExtension
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configure Room-specific options
 */
internal fun Project.configureKmpRoom(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    extensions.configure<KotlinMultiplatformExtension> {
//      No longer needed but a good example for how to create custom source sets
//        // Apply the default hierarchy again. It'll create, for example, the iosMain source set:
//        // https://kotlinlang.org/docs/multiplatform-hierarchy.html
//        applyDefaultHierarchyTemplate()
//
//        sourceSets.create("commonDesktopIosAndroid") {
//            dependsOn(sourceSets.commonMain.get())
//            sourceSets.iosMain.get().dependsOn(this)
//            sourceSets.androidMain.get().dependsOn(this)
//            sourceSets.named("desktopMain").get().dependsOn(this)
//
//            dependencies {
//
//            }
//        }

        sourceSets.androidMain.dependencies {
        }
        sourceSets.commonMain.dependencies {
            // Room
            implementation(libs.findBundle("room.common.kmp").get())
        }

    }

    commonExtension.apply {
        dependencies {
            add("kspAndroid", libs.findLibrary("room.compiler").get())
            add("kspDesktop", libs.findLibrary("room.compiler").get())
            add("kspIosX64", libs.findLibrary("room.compiler").get())
            add("kspIosArm64", libs.findLibrary("room.compiler").get())
            add("kspIosSimulatorArm64", libs.findLibrary("room.compiler").get())
        }
    }

    extensions.configure<RoomExtension> {
        schemaDirectory("$projectDir/schemas")
    }
}