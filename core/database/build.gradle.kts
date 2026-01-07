plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.phovo.kmp.library.room)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.database"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logger)
            implementation(projects.core.common)
            implementation(projects.core.model)
        }
    }
}
