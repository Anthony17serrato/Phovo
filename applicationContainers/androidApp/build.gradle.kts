plugins {
    alias(libs.plugins.phovo.kmp.android.application)
    alias(libs.plugins.phovo.kmp.android.application.koin)
    alias(libs.plugins.phovo.kmp.library.compose)
    alias(libs.plugins.serialization)
    // TODO: Build flavors on pure android module is
    //  currently not working due to AGP 9 updates
    //alias(libs.plugins.phovo.kmp.build.flavors)
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.sharedumbrella)
    implementation(libs.compose.resources)
    implementation(libs.platformtools.darkmodedetector)
    implementation(libs.androidx.splash)
    implementation(libs.androidx.activity.compose)
}