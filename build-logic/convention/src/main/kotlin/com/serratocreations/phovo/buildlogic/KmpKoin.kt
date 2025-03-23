package com.serratocreations.phovo.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Configure Koin-specific options
 */
internal fun Project.configureKmpKoin(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    extensions.configure<KotlinMultiplatformExtension> {
        jvm("desktop")

        androidTarget()

        @OptIn(ExperimentalWasmDsl::class)
        wasmJs()

        iosX64()
        iosArm64()
        iosSimulatorArm64()

        sourceSets.androidMain.dependencies {
            implementation(libs.findLibrary("koin.android").get())
        }
        sourceSets.commonMain.dependencies {
            // Koin
            implementation(libs.findBundle("koin.common.kmp").get())
            // Koin Annotations
            api(libs.findLibrary("koin.annotations").get())
        }

        // KSP Common sourceSet https://insert-koin.io/docs/setup/annotations/
        sourceSets.named("commonMain").configure {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
    }

    commonExtension.apply {
        dependencies {
            add("kspCommonMainMetadata", libs.findLibrary("koin.ksp.compiler").get())
            add("kspAndroid", libs.findLibrary("koin.ksp.compiler").get())
            add("kspWasmJs", libs.findLibrary("koin.ksp.compiler").get())
            add("kspDesktop", libs.findLibrary("koin.ksp.compiler").get())
            add("kspIosX64", libs.findLibrary("koin.ksp.compiler").get())
            add("kspIosArm64", libs.findLibrary("koin.ksp.compiler").get())
            add("kspIosSimulatorArm64", libs.findLibrary("koin.ksp.compiler").get())
        }
    }

    // Trigger Common Metadata Generation from Native tasks
    project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
        if (name != "kspCommonMainKotlinMetadata") {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }


//    project.tasks.matching { it.name.contains("ksp") }.configureEach {
//        if (name != "kspCommonMainKotlinMetadata") {
//            dependsOn("kspCommonMainKotlinMetadata")
//        }
//    }

    extensions.configure<KspExtension> {
        // Enable Koin Viewmodel Annotation
        arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
        // Does not work properly with KMP ComponentScan
        //arg("KOIN_CONFIG_CHECK", "true")
        arg("KOIN_LOG_TIMES", "true")
    }
}