plugins {
    id(libs.plugins.phovo.kmp.application.koin.get().pluginId)
    id(libs.plugins.phovo.kmp.application.compose.get().pluginId)
    id(libs.plugins.phovo.kmp.application.application.get().pluginId)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            // Project dependencies
            implementation(projects.core.designsystem)
            implementation(projects.feature.photos)
            implementation(projects.feature.connections)

            implementation(compose.components.resources)
            implementation(libs.serialization.json)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.swing)
            implementation("io.ktor:ktor-server-status-pages:3.0.2")
            implementation("io.ktor:ktor-server-core:3.0.2")  // Ktor core server
            implementation("io.ktor:ktor-server-netty:3.0.2") // Netty engine for Ktor server
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")  // JSON serialization
            implementation("io.ktor:ktor-server-content-negotiation:3.0.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")  // Kotlinx Serialization library
        }
    }
}

android {
    namespace = "com.serratocreations.phovo"

    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}
