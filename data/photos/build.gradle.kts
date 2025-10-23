plugins {
    id(libs.plugins.phovo.kmp.android.ios.desktop.web.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        val commonMain by getting {
            dependencies {
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
                // Todo Use version catalog instead
                implementation("com.ashampoo:kim:0.20")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                // TODO: Replace with ffmpeg
                implementation("org.apache.tika:tika-core:3.2.2")
                implementation("org.apache.tika:tika-parsers-standard-package:3.2.2")
            }
        }
        iosMain {
            dependencies {
                implementation(libs.skiko)
                implementation(libs.ktor.client.darwin)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
        val commonDesktopIosAndroid by getting {
            dependencies {
                implementation(projects.core.database)
            }
        }
    }
}

android {
    namespace = "com.serratocreations.phovo.data.photos"
}