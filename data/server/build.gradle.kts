plugins {
    id(libs.plugins.phovo.kmp.android.ios.desktop.web.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId)
    id(libs.plugins.phovo.kmp.library.compose.get().pluginId)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        val androidMain by getting

        val commonMain by getting {
            dependencies {
                implementation(projects.core.common)
                implementation(projects.core.logger)
                implementation(projects.core.model)
                implementation(projects.data.photos)
                implementation(libs.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(projects.core.database)
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
    namespace = "com.serratocreations.phovo.data.server"
}
