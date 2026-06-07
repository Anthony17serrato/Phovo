import com.serratocreations.phovo.buildlogic.getBuildFlavor

plugins {
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.phovo.kmp.library.compose)
    alias(libs.plugins.phovo.kmp.umbrella.library)
    alias(libs.plugins.serialization)
    alias(libs.plugins.phovo.kmp.build.flavors)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.sharedumbrella"
    }
    val buildFlavor = getBuildFlavor()
    sourceSets {
        androidMain.dependencies {

        }
        commonDesktopIosAndroid.dependencies {
            if(buildFlavor == com.serratocreations.phovo.buildlogic.Flavor.Dev) {
                implementation(projects.core.database)
            }
        }
        commonMain.dependencies {
            // Project dependencies
            implementation(projects.core.designsystem)
            implementation(projects.feature.photos)
            implementation(projects.feature.connections)
            implementation(projects.core.common)
            implementation(projects.core.logger)
            implementation(projects.core.model)
            implementation(projects.core.serverconfig)
            implementation(projects.data.server)
            implementation(projects.data.photos)
            implementation(projects.core.navigation)

            implementation(libs.compose.resources)
            implementation(libs.serialization.json)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.bundles.navigation3)
            implementation(libs.filekit.core)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.swing)
        }
    }
}
