plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.serialization)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.data.photos"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.exifinterface)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.video)
        }

        commonMain.dependencies {
            implementation(projects.core.database)
            implementation(projects.core.common)
            implementation(projects.core.logger)
            implementation(projects.core.model)
            implementation(projects.core.serverconfig)
            implementation(libs.serialization.json)
            implementation(libs.kotlin.datetime)
            implementation(libs.coil.compose)
            implementation(libs.filekit.coil)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.filekit.core)
            implementation(libs.kotlinx.io)
        }

        commonIosAndroid.dependencies {
            implementation(projects.data.permissions)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(projects.data.thumbnails)
            implementation(libs.metadata.extractor)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}