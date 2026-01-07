package com.serratocreations.phovo.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configure Room-specific options
 */
internal fun Project.configureKmpRoom() {
    extensions.configure<KotlinMultiplatformExtension> {

        sourceSets.androidMain.dependencies {
        }
        sourceSets.commonMain.dependencies {
            // Room
            implementation(libs.findBundle("room.common.kmp").get())
        }

    }

    this.apply {
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