package com.serratocreations.phovo.buildlogic

import androidx.room.gradle.RoomExtension
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Configure Room-specific options
 */
internal fun Project.configureKmpRoom(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    extensions.configure<KotlinMultiplatformExtension> {
        jvm("desktop")

        androidTarget()

        iosX64()
        iosArm64()
        iosSimulatorArm64()

        // Apply the default hierarchy again. It'll create, for example, the iosMain source set:
        // https://kotlinlang.org/docs/multiplatform-hierarchy.html
        applyDefaultHierarchyTemplate()

        sourceSets.create("commonDesktopIosAndroid") {
            dependsOn(sourceSets.commonMain.get())
            sourceSets.iosMain.get().dependsOn(this)
            sourceSets.androidMain.get().dependsOn(this)
            sourceSets.named("desktopMain").get().dependsOn(this)

            dependencies {
                // Room
                implementation(libs.findBundle("room.common.kmp").get())
            }
        }

        sourceSets.androidMain.dependencies {

        }
        sourceSets.commonMain.dependencies {

        }

        // KSP Common sourceSet https://insert-koin.io/docs/setup/annotations/
        sourceSets.named("commonMain").configure {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
    }

    commonExtension.apply {
        dependencies {
            add("kspCommonMainMetadata", libs.findLibrary("room.compiler").get())
            add("kspAndroid", libs.findLibrary("room.compiler").get())
            add("kspDesktop", libs.findLibrary("room.compiler").get())
            add("kspIosX64", libs.findLibrary("room.compiler").get())
            add("kspIosArm64", libs.findLibrary("room.compiler").get())
            add("kspIosSimulatorArm64", libs.findLibrary("room.compiler").get())
        }
    }

    // Trigger Common Metadata Generation from Native tasks
    project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
        if (name != "kspCommonMainKotlinMetadata") {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }

    extensions.configure<RoomExtension> {
        schemaDirectory("$projectDir/schemas")
    }
}