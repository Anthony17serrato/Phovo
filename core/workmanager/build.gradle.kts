plugins {
    alias(libs.plugins.phovo.kmp.android.ios.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.serialization)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.workmanager"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logger)
            implementation(projects.core.common)
            implementation(libs.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
