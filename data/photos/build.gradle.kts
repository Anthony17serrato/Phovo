plugins {
    id(libs.plugins.phovo.kmp.library.library.get().pluginId)
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
                implementation(libs.serialization.json)
                implementation(libs.kotlin.datetime)
                implementation(libs.coil.compose)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.serialization)
                implementation(libs.ktor.client.content.negotiation)
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
            }
        }
        iosMain {
            dependencies {
                implementation(libs.skiko)
            }
        }
    }
}

android {
    namespace = "com.serratocreations.phovo.data.photos"
}
