package com.serratocreations.phovo.buildlogic

import com.serratocreations.phovo.buildlogic.extension.getTargetPlatforms
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureBuildFlavors() {
    configure<KotlinMultiplatformExtension> {
        val targets = getTargetPlatforms()
        val flavor = getBuildFlavor()
        if (targets.contains(Targets.ANDROID)) {
            // Build flavors configuration
            sourceSets.androidMain.get().apply {
                // Hook the appropriate source set
                when(flavor) {
                    Flavor.Dev -> kotlin.srcDirs("src/androidDev/kotlin")
                    Flavor.Prod -> kotlin.srcDirs("src/androidProd/kotlin")
                }
                // can do something similar for resources too if required
            }
        }
        if (targets.contains(Targets.IOS)) {
            sourceSets.iosMain.get().apply {
                // Hook the appropriate source set
                when(flavor) {
                    Flavor.Dev -> kotlin.srcDirs("src/iosDev/kotlin")
                    Flavor.Prod -> kotlin.srcDirs("src/iosProd/kotlin")
                }
                // can do something similar for resources too if required
            }
        }
        if (targets.contains(Targets.WEB)) {
            sourceSets.webMain.get().apply {
                // Hook the appropriate source set
                when(flavor) {
                    Flavor.Dev -> kotlin.srcDirs("src/webDev/kotlin")
                    Flavor.Prod -> kotlin.srcDirs("src/webProd/kotlin")
                }
                // can do something similar for resources too if required
            }
        }
        // NOTE: for JVM the source dir is nested inside of the jvmMain source set, otherwise the IDE
        // could not resolve the directory correctly
        if (targets.contains(Targets.DESKTOP)) {
            sourceSets.jvmMain.get().apply {
                // Hook the appropriate source set
                when(flavor) {
                    Flavor.Dev -> kotlin.srcDir("src/jvmMain/dev/kotlin")
                    Flavor.Prod -> kotlin.srcDir("src/jvmMain/prod/kotlin")
                }
                // can do something similar for resources too if required
            }
        }
        sourceSets.commonMain.get().apply {
            // Hook the appropriate source set
            when(flavor) {
                Flavor.Dev -> kotlin.srcDirs("src/commonDev/kotlin")
                Flavor.Prod -> kotlin.srcDirs("src/commonProd/kotlin")
            }
            // can do something similar for resources too if required
        }
    }
}