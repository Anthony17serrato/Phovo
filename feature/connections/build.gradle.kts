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

                implementation(compose.components.resources)
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
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.status.pages)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.serialization)
                implementation(libs.ktor.server.content.negotiation)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:3.0.2")
            }
        }
    }
}

android {
    namespace = "com.serratocreations.phovo.feature.connections"
}
