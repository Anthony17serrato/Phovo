plugins {
    id(libs.plugins.phovo.kmp.android.ios.desktop.web.library.get().pluginId)
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
                implementation(projects.data.server)

                implementation(libs.compose.resources)
                implementation(libs.serialization.json)
                implementation(libs.coil.compose)
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val desktopMain by getting {
            dependencies {

            }
        }
        val wasmJsMain by getting {
            dependencies {

            }
        }
    }
}

android {
    namespace = "com.serratocreations.phovo.feature.connections"
}
