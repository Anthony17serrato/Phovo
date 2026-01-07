plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.common"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logger)
            implementation(libs.coil.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}