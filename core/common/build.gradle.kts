import com.serratocreations.phovo.buildlogic.generatePhovoBuildInfo

plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.phovo.kmp.library.compose)
}

// PhovoBuildInfo: the app version and build flavor, readable from common code.
generatePhovoBuildInfo(packageName = "com.serratocreations.phovo.core.common")

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.common"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logger)
            implementation(libs.coil.compose)
            implementation(libs.filekit.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}