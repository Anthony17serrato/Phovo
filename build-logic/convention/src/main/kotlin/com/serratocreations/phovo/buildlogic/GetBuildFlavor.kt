package com.serratocreations.phovo.buildlogic

import org.gradle.api.Project

/**
 * The build flavor: the `phovo.flavor` Gradle property if set (`-Pphovo.flavor=prod`, which release
 * builds pass), otherwise `FLAVOR` in `gradle/flavor.properties`, which stays `dev` for local work.
 */
fun Project.getBuildFlavor(): Flavor {
    val flavor = providers.gradleProperty("phovo.flavor").orNull ?: run {
        // Read through Gradle so no file handle stays open in the daemon, the same way
        // phovoVersion() reads version.properties.
        val file = rootProject.layout.projectDirectory.file("gradle/flavor.properties")
        val text = providers.fileContents(file).asText.get()
        java.util.Properties().apply { load(text.reader()) }.getProperty("FLAVOR")
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
