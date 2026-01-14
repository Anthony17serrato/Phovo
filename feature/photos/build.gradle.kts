plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.phovo.kmp.library.compose)
    alias(libs.plugins.serialization)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.feature.photos"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.coil.video)
            implementation(libs.exoplayer)
            implementation(libs.media.ui)
        }

        commonMain.dependencies {
            implementation(projects.core.designsystem)
            implementation(projects.core.common)
            implementation(projects.core.logger)
            implementation(projects.data.photos)
            implementation(projects.data.server)
            implementation(projects.core.navigation)

            implementation(libs.compose.resources)
            implementation(libs.serialization.json)
            implementation(libs.coil.compose)
            implementation(libs.kotlin.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        iosMain.dependencies {
            implementation(libs.skiko)
        }

        desktopMain.dependencies {
            implementation(projects.data.ffmpeg)
        }
    }
}