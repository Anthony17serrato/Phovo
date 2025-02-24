plugins {
    id(libs.plugins.phovo.kmp.library.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId)
    id(libs.plugins.phovo.kmp.library.compose.get().pluginId)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.core.designsystem)
                implementation(projects.core.common)
                implementation(projects.data.photos)
                implementation(projects.data.server)

                implementation(compose.components.resources)
                implementation(libs.serialization.json)
                implementation(libs.coil.compose)
                implementation(libs.kotlin.datetime)
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
    namespace = "com.serratocreations.phovo.feature.photos"
}
