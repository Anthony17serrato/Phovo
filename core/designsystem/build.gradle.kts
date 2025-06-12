plugins {
    id(libs.plugins.phovo.kmp.android.ios.desktop.web.library.get().pluginId)
    id(libs.plugins.phovo.kmp.library.compose.get().pluginId)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                implementation(compose.components.resources)
                implementation(projects.core.common)
                api(compose.components.uiToolingPreview)
                api(compose.materialIconsExtended)
                api(compose.foundation)
                api(compose.material3)
                api(compose.material3AdaptiveNavigationSuite)
                api(compose.runtime)
                api(compose.ui)
                api(libs.compose.adaptive)
                api(libs.compose.adaptive.layout)
                api(libs.compose.adaptive.navigation)
                api(libs.compose.foundation.layout)
                api(libs.compose.material.navigation)
                api(libs.compose.navigation)
                api(libs.compose.navigation.common)
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
    namespace = "com.serratocreations.phovo.core.designsystem"
}
