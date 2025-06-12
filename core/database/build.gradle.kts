plugins {
    id(libs.plugins.phovo.kmp.android.ios.desktop.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.koin.get().pluginId)
    id(libs.plugins.phovo.kmp.library.room.get().pluginId)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.core.logger)
                implementation(projects.core.common)
            }
        }
//        val commonDesktopIosAndroid by getting {
//            dependencies {
//
//            }
//        }
    }
}

android {
    namespace = "com.serratocreations.phovo.core.database"
}
