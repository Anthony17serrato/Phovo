plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.domain"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.data.photos)
            implementation(projects.data.server)
            implementation(projects.core.logger)
            implementation(projects.core.common)
            implementation(libs.filekit.core)
            implementation(libs.kotlin.datetime)
            // It is used only for creating URIs and the coil URI
            // TODO Investigate if there is any other KMP library dedicated to just URI(FileKit does not seem to support it)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
        }
        jvmMain.dependencies {
            implementation(projects.data.thumbnails)
        }
    }
}
