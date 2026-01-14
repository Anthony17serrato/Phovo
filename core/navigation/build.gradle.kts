plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.serialization)
    alias(libs.plugins.phovo.kmp.library.compose)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.navigation"
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.navigation3.runtime)
            //implementation(libs.androidx.savedstate.compose)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
        }
        commonTest.dependencies {
        }
    }
}
