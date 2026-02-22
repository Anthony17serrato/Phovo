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
        }
        commonTest.dependencies {
        }
    }
}
