plugins {
    id(libs.plugins.phovo.kmp.library.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.serialization.json)
                implementation(libs.kotlin.datetime)
                implementation(libs.coil.compose)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
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
