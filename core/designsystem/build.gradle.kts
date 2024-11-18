plugins {
    id(libs.plugins.phovo.kmp.library.library.get().pluginId)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                implementation(compose.components.resources)
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
