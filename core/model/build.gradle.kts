plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.serialization)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.model"
    }
    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
        }
    }
}
