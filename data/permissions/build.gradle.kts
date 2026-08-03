plugins {
    alias(libs.plugins.phovo.kmp.android.ios.library)
    alias(libs.plugins.phovo.kmp.library.koin)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.data.permissions"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logger)
            implementation(projects.core.database)
            implementation(projects.core.common)
        }
    }
}