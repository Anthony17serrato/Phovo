plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.serialization)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.data.photos"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.logger)
            implementation(projects.core.model)
            implementation(libs.serialization.json)
            implementation(libs.kotlin.datetime)
            implementation(libs.coil.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.filekit.core)
            implementation(libs.kotlinx.io)
            // TODO: Replace with ffmpeg
            implementation("com.ashampoo:kim:0.20")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // TODO: Replace with ffmpeg
            implementation("org.apache.tika:tika-core:3.2.2")
            implementation("org.apache.tika:tika-parsers-standard-package:3.2.2")
        }

        iosMain.dependencies {
            implementation(libs.skiko)
            implementation(libs.ktor.client.darwin)
        }

        webMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        commonDesktopIosAndroid.dependencies {
            implementation(projects.core.database)
        }
    }
}