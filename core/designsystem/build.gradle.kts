plugins {
    alias(libs.plugins.phovo.kmp.android.ios.desktop.web.library)
    alias(libs.plugins.phovo.kmp.library.compose)
}

kotlin {
    android {
        namespace = "com.serratocreations.phovo.core.designsystem"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)
            implementation(projects.core.common)
            implementation(libs.platformtools.darkmodedetector)
            api(libs.compose.ui)
            api(libs.material.icons.extended)
            api(libs.compose.foundation)
            api(libs.material3)
            api(libs.material3.adaptive.navigation.suite)
            api(libs.compose.runtime)
            api(libs.compose.ui.tooling.preview)
            api(libs.lifecycle.runtime.compose)
            api(libs.compose.adaptive)
            api(libs.compose.adaptive.layout)
            api(libs.compose.adaptive.navigation)
            api(libs.compose.foundation.layout)
            api(libs.compose.material.navigation)
            api(libs.compose.navigation)
            api(libs.compose.navigation.common)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
