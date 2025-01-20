plugins {
    id(libs.plugins.phovo.kmp.library.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                //implementation(libs.coroutines.core)
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
    namespace = "com.serratocreations.phovo.core.common"
}
