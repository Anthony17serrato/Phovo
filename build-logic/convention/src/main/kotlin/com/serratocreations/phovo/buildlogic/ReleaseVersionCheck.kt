package com.serratocreations.phovo.buildlogic

import org.gradle.api.GradleException

/**
 * Why [version] can't be released from [branch], given the repository's [existingTags]. Empty when
 * it can. The release workflow runs this (through the root `checkReleaseVersion` task) before
 * building anything.
 *
 * - The tag `v<version>` must not exist yet.
 * - The version must be newer than every existing release, compared by versionCode. Android and
 *   Windows Installer refuse to install a lower number over a higher one, so publishing an older
 *   version would strand everyone who installed the newer one.
 * - Stable versions come from `main`; alphas, betas and release candidates from `develop` or a
 *   `release/x.y` beta line. See "Branching and Releases" in the README.
 */
fun releaseVersionProblems(
    version: PhovoVersion,
    branch: String,
    existingTags: Collection<String>,
): List<String> = buildList {
    val tag = "v${version.name}"
    if (tag in existingTags) add("The tag $tag already exists. Bump versionName in version.properties.")

    val newer = existingTags
        .filter { it.startsWith("v") && it != tag }
        // Tags outside the scheme can't be compared, so they don't block a release.
        .mapNotNull { existing ->
            try {
                PhovoVersion.parse(existing.removePrefix("v"))
            } catch (_: GradleException) {
                null
            }
        }
        .filter { it.versionCode >= version.versionCode }
        .sortedBy { it.versionCode }
    if (newer.isNotEmpty()) {
        add(
            "$tag isn't newer than the existing release(s) " + newer.joinToString { "v${it.name}" } +
                ". Installs of those builds would refuse it as a downgrade."
        )
    }

    when {
        branch == "main" -> if (version.isPrerelease) {
            add("$tag is a pre-release. main only publishes stable versions; release it from develop or release/*.")
        }
        branch == "develop" || branch.startsWith("release/") -> if (!version.isPrerelease) {
            add("$tag is a stable version. Stable releases are published from main.")
        }
        else -> add("Releases are published from main, develop or release/*, not $branch.")
    }
}
