package com.serratocreations.phovo.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Configure Compose-specific options
 */
internal fun Project.configureKmpCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }
//      Todo: investigate appropriate tooling dependencies
//        dependencies {
//            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
//            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
//        }

//        testOptions {
//            unitTests {
//                // For Robolectric
//                isIncludeAndroidResources = true
//            }
//        }
    }

    // Configure Compose resources
    extensions.configure<ComposeExtension> {
        configure<ResourcesExtension> {
            publicResClass = true
            generateResClass = auto
        }
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
//        fun Provider<String>.onlyIfTrue() = flatMap { provider { it.takeIf(String::toBoolean) } }
//        fun Provider<*>.relativeToRootProject(dir: String) = flatMap {
//            rootProject.layout.buildDirectory.dir(projectDir.toRelativeString(rootDir))
//        }.map { it.dir(dir) }
//
//        project.providers.gradleProperty("enableComposeCompilerMetrics").onlyIfTrue()
//            .relativeToRootProject("compose-metrics")
//            .let(metricsDestination::set)
//
//        project.providers.gradleProperty("enableComposeCompilerReports").onlyIfTrue()
//            .relativeToRootProject("compose-reports")
//            .let(reportsDestination::set)
//
//        stabilityConfigurationFile =
//            rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
    }
}