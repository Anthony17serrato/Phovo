plugins {
    alias(libs.plugins.phovo.kmp.web.application)
    alias(libs.plugins.phovo.kmp.library.compose)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.material3)
        }
    }
}
