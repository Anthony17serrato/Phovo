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
                implementation(projects.data.photos)
                implementation(projects.core.common)

                implementation(compose.components.resources)
                implementation(libs.serialization.json)
                implementation(libs.coil.compose)
                implementation(libs.filekit.core)
                implementation(libs.filekit.compose)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.status.pages)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.serialization)
                implementation(libs.ktor.server.content.negotiation)
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
