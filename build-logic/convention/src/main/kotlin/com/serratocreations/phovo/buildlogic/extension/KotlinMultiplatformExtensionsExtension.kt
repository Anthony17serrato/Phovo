package com.serratocreations.phovo.buildlogic.extension

import com.serratocreations.phovo.buildlogic.Targets
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.getTargetPlatforms(): Set<Targets> {
    val configuredTargets = mutableSetOf<Targets>()
    val sourceSetNames = sourceSets.names

    // Check for iOS targets
    if (sourceSetNames.any { it.startsWith("ios")}) {
        configuredTargets.add(Targets.IOS)
    }

    // Check for Desktop target
    if (sourceSetNames.any { it.contains("desktopMain")}) {
        configuredTargets.add(Targets.DESKTOP)
    }

    // Check for WASM target
    if (sourceSetNames.any {
        it.contains("wasmJsMain") || it.contains("webMain")
    }) {
        configuredTargets.add(Targets.WEB)
    }
    // Check for Android target
    if (sourceSetNames.any { it.contains("androidMain")}) {
        configuredTargets.add(Targets.ANDROID)
    }
    return configuredTargets
}