plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.library)
    alias(libs.plugins.phovo.kmp.library.koin)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.serverconfig"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.filekit.core)
            implementation(projects.core.model)
        }
        jvmMain.dependencies {
            implementation(projects.core.database)
        }
        commonTest.dependencies {
        }
    }
}
