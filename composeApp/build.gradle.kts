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
        }
        commonMain.dependencies {
            // Project dependencies
            implementation(projects.core.designsystem)
            implementation(projects.feature.photos)
            implementation(projects.feature.connections)
            implementation(projects.core.common)
            implementation(projects.core.logger)
            implementation(projects.data.server)
            implementation(projects.data.photos)

            implementation(compose.components.resources)
            implementation(libs.serialization.json)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.swing)
        }
    }
}

android {
    namespace = "com.serratocreations.phovo"
}

// File picker desktop config
compose.desktop {
    application {
        nativeDistributions {
            linux {
                modules("jdk.security.auth")
            }
        }
    }
}
