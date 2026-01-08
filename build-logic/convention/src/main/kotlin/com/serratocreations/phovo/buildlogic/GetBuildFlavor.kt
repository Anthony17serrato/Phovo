package com.serratocreations.phovo.buildlogic

import org.gradle.api.Project

fun Project.getBuildFlavor(): Flavor {
    val flavorProperties = java.util.Properties().apply {
        load(rootProject.file("gradle/flavor.properties").inputStream())
    }
    return flavorProperties.getProperty("FLAVOR").let { flavor ->
        when(flavor) {
            Flavor.Dev.flavorName -> Flavor.Dev
            Flavor.Prod.flavorName -> Flavor.Prod
            else -> {
                println("Unknown flavor: $flavor")
                Flavor.Prod
            }
        }
    }
}