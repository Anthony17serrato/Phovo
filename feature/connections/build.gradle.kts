plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.phovo.kmp.library.compose)
    alias(libs.plugins.serialization)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.feature.connections"
    }
    sourceSets {
        commonMain.dependencies {
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

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        desktopMain.dependencies {

        }

        wasmJsMain.dependencies {

        }
    }
}
