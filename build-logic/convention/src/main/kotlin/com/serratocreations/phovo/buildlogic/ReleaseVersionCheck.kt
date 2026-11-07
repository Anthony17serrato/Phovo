package com.serratocreations.phovo.buildlogic

import org.gradle.api.GradleException

/**
 * Why [version] can't be released from [branch], given the repository's [existingTags]. Empty when
 * it can. The release workflow runs this (through the root `checkReleaseVersion` task) before
 * building anything.
 *
 * - The tag `v<version>` must not exist yet.
 * - The version must be newer, by versionCode, than every release it could be installed over.
 *   Android and Windows Installer refuse a lower number over a higher one. Which releases count
 *   depends on the version:
 *   - A stable version is compared with the earlier stable releases. Stable users only have those.
 *     A newer beta or alpha line doesn't count: its testers are never offered the stable patch,
 *     because it sorts lower, so a 1.0.1 hotfix can ship after 1.1.0-beta.1.
 *   - A pre-release is compared with every release on its own major.minor line, so 1.1.0-beta.3
 *     must follow 1.1.0-beta.2 and can't come after 1.1.0. Other lines don't count, so release/1.1
 *     can keep shipping betas after develop tags 1.2.0-alpha.1.
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

    val released = existingTags
        .filter { it.startsWith("v") && it != tag }
        // Tags outside the scheme can't be compared, so they don't block a release.
        .mapNotNull { existing ->
            try {
                PhovoVersion.parse(existing.removePrefix("v"))
            } catch (_: GradleException) {
                null
            }
        }
    val installedOver = if (version.isPrerelease) {
        released.filter { it.major == version.major && it.minor == version.minor }
    } else {
        released.filter { !it.isPrerelease }
    }
    val newer = installedOver.filter { it.versionCode >= version.versionCode }.sortedBy { it.versionCode }
    if (newer.isNotEmpty()) {
        add(
            "$tag isn't newer than " + newer.joinToString { "v${it.name}" } +
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
