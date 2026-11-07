package com.serratocreations.phovo.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Where the Android release key comes from. Each value is read from a Gradle property (for example
 * in `~/.gradle/gradle.properties`) or, failing that, an environment variable (what CI sets). The
 * keystore and its passwords never live in the repository.
 */
private enum class SigningValue(val property: String, val environmentVariable: String) {
    StoreFile("phovo.signing.storeFile", "PHOVO_SIGNING_STORE_FILE"),
    StorePassword("phovo.signing.storePassword", "PHOVO_SIGNING_STORE_PASSWORD"),
    KeyAlias("phovo.signing.keyAlias", "PHOVO_SIGNING_KEY_ALIAS"),
    KeyPassword("phovo.signing.keyPassword", "PHOVO_SIGNING_KEY_PASSWORD"),
}

/** Fails the build instead of falling back to the debug key. The release workflow sets it. */
private const val REQUIRE_RELEASE_SIGNING = "phovo.requireReleaseSigning"

/**
 * Signs release builds with the release key when it's configured. Without it, local release builds
 * fall back to the debug key, unless `-Pphovo.requireReleaseSigning=true` is passed, which makes a
 * missing key a build failure so a debug-signed APK can't be published by accident.
 *
 * Every release has to be signed with the same key: Android won't install an update signed with a
 * different one, so testers would have to uninstall and lose their data. See "Android release
 * signing" in the README.
 */
internal fun ApplicationExtension.configureReleaseSigning(project: Project) {
    val values = SigningValue.entries.associateWith { value ->
        // A blank property (say `phovo.signing.keyPassword=` left in gradle.properties) counts as
        // unset, so the environment variable still gets a chance.
        project.providers.gradleProperty(value.property).filter { it.isNotBlank() }
            .orElse(project.providers.environmentVariable(value.environmentVariable).filter { it.isNotBlank() })
            .orNull
    }
    val required = project.providers.gradleProperty(REQUIRE_RELEASE_SIGNING).orNull.toBoolean()
    val missing = values.filterValues { it == null }.keys

    val releaseSigning = if (missing.isEmpty()) {
        signingConfigs.create("release") {
            // Relative paths resolve against the repository root, not the app module. A missing
            // file is left to AGP's validateSigningRelease task, which only runs when a release is
            // packaged, so a stale path doesn't break builds that never sign anything.
            storeFile = project.rootProject.file(values.getValue(SigningValue.StoreFile)!!)
            storePassword = values.getValue(SigningValue.StorePassword)
            keyAlias = values.getValue(SigningValue.KeyAlias)
            keyPassword = values.getValue(SigningValue.KeyPassword)
        }
    } else if (required) {
        throw GradleException(
            "$REQUIRE_RELEASE_SIGNING is set but the Android release key isn't fully configured. " +
                "Missing: " + missing.joinToString { "${it.property} (or ${it.environmentVariable})" } + "."
        )
    } else {
        // Some but not all values set is almost always a mistake, so say so instead of quietly
        // signing with the debug key. Nothing set at all is the normal local setup.
        if (missing.size < SigningValue.entries.size) {
            project.logger.warn(
                "Android release key is only partly configured, so release builds use the debug key. " +
                    "Missing: " + missing.joinToString { "${it.property} (or ${it.environmentVariable})" } + "."
            )
        }
        null
    }

    buildTypes.getByName("release") {
        signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
    }
}
