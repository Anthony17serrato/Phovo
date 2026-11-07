import com.serratocreations.phovo.buildlogic.getBuildFlavor
import com.serratocreations.phovo.buildlogic.phovoVersion
import com.serratocreations.phovo.buildlogic.releaseVersionProblems

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library) apply false
    alias(libs.plugins.phovo.kmp.android.ios.desktop.library) apply false
    alias(libs.plugins.phovo.kmp.android.ios.library) apply false
    alias(libs.plugins.phovo.kmp.umbrella.library) apply false
    alias(libs.plugins.phovo.kmp.library.koin) apply false
    alias(libs.plugins.phovo.kmp.library.compose) apply false
    alias(libs.plugins.phovo.kmp.library.room) apply false
    alias(libs.plugins.phovo.kmp.desktop.library) apply false
    alias(libs.plugins.phovo.kmp.desktop.application) apply false
    alias(libs.plugins.phovo.kmp.android.application) apply false
}

// Prints the app version for the release workflow, so the versionCode formula lives in one place
// (build-logic's PhovoVersion.kt). Run with -q to get just the key=value lines.
tasks.register("printVersion") {
    val version = phovoVersion()
    val flavor = getBuildFlavor().flavorName
    doLast {
        println("versionName=${version.name}")
        println("versionCode=${version.versionCode}")
        println("desktopPackageVersion=${version.desktopPackageVersion}")
        println("prerelease=${version.isPrerelease}")
        println("flavor=$flavor")
    }
}

// Fails when version.properties can't be released from -Pphovo.releaseBranch: the tag exists, the
// version isn't newer than every v* tag, or its stage doesn't fit the branch. The release workflow
// runs it before building anything. See ReleaseVersionCheck.kt.
tasks.register("checkReleaseVersion") {
    val version = phovoVersion()
    val branch = providers.gradleProperty("phovo.releaseBranch")
    val tags = providers.exec { commandLine("git", "tag", "--list", "v*") }
        .standardOutput.asText.map { it.lines().map(String::trim).filter(String::isNotEmpty) }
    doLast {
        val releaseBranch = branch.orNull
            ?: throw GradleException("Pass the branch being released: -Pphovo.releaseBranch=<branch>.")
        val problems = releaseVersionProblems(version, releaseBranch, tags.get())
        if (problems.isNotEmpty()) {
            throw GradleException("v${version.name} can't be released from $releaseBranch:\n- " + problems.joinToString("\n- "))
        }
        println("v${version.name} can be released from $releaseBranch.")
    }
}
