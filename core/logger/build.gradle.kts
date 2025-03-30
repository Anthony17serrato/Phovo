plugins {
    id(libs.plugins.phovo.kmp.library.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kermit.logging)
                api(libs.kermit.logging.koin)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.serratocreations.phovo.core.logger"
}
