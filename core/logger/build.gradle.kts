plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.logger"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kermit.logging)
            implementation(libs.kotlin.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
