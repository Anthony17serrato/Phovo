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
                implementation("io.ktor:ktor-server-status-pages:3.0.2")
                implementation("io.ktor:ktor-server-core:3.0.2")  // Ktor core server
                implementation("io.ktor:ktor-server-netty:3.0.2") // Netty engine for Ktor server
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")  // JSON serialization
                implementation("io.ktor:ktor-server-content-negotiation:3.0.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")  // Kotlinx Serialization library
            }
        }
    }
}

android {
    namespace = "com.serratocreations.phovo.feature.connections"
}
