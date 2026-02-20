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
            implementation(libs.bundles.navigation3)
            implementation(libs.material.icons.extended)
            implementation(libs.material3)
            //implementation(libs.androidx.savedstate.compose)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
        }
        commonTest.dependencies {
        }
    }
}
