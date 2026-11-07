package com.serratocreations.phovo.buildlogic

import org.gradle.api.Project

/**
 * The build flavor: the `phovo.flavor` Gradle property if set (`-Pphovo.flavor=prod`, which release
 * builds pass), otherwise `FLAVOR` in `gradle/flavor.properties`, which stays `dev` for local work.
 */
fun Project.getBuildFlavor(): Flavor {
    val flavor = providers.gradleProperty("phovo.flavor").orNull ?: run {
        val flavorProperties = java.util.Properties().apply {
            load(rootProject.file("gradle/flavor.properties").inputStream())
        }
        flavorProperties.getProperty("FLAVOR")
    }
    return when(flavor) {
        Flavor.Dev.flavorName -> Flavor.Dev
        Flavor.Prod.flavorName -> Flavor.Prod
        else -> {
            println("Unknown flavor: $flavor")
            Flavor.Prod
        }
    }
}
