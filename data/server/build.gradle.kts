plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.phovo.kmp.library.compose)
    alias(libs.plugins.serialization)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.data.server"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.logger)
            implementation(projects.core.model)
            implementation(projects.data.photos)
            implementation(libs.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(projects.core.database)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.status.pages)
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.server.content.negotiation)
        }

        webMain.dependencies {

        }
    }
}