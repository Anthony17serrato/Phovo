
plugins {
    alias(libs.plugins.phovo.kmp.web.application)
    alias(libs.plugins.phovo.kmp.library.koin)
    alias(libs.plugins.phovo.kmp.library.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.phovo.kmp.build.flavors)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.sharedumbrella)
            implementation(libs.compose.resources)
            implementation(libs.platformtools.darkmodedetector)
        }
    }
}