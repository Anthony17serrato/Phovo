import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id(libs.plugins.phovo.kmp.application.koin.get().pluginId)
    id(libs.plugins.phovo.kmp.application.compose.get().pluginId)
    id(libs.plugins.phovo.kmp.application.application.get().pluginId)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(libs.androidx.splash)
        }
        commonMain.dependencies {
            // Project dependencies
            implementation(projects.core.designsystem)
            implementation(projects.feature.photos)
            implementation(projects.feature.connections)
            implementation(projects.core.common)
            implementation(projects.core.logger)
            implementation(projects.data.server)
            implementation(projects.data.photos)

            implementation(libs.compose.resources)
            implementation(libs.serialization.json)
            implementation(libs.platformtools.darkmodedetector)
            implementation(libs.navigation3.ui)
            implementation(libs.navigation3.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.material3.adaptive.navigation3)
            implementation(libs.kotlinx.serialization.core)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.swing)
        }
    }
}

android {
    namespace = "com.serratocreations.phovo"
}

// File picker desktop config
compose.desktop {
    application {
        mainClass = "com.serratocreations.phovo.MainKt"

        nativeDistributions {
            linux {
                modules("jdk.security.auth")
            }
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.serratocreations.phovo"
            packageVersion = "1.0.0"
            appResourcesRootDir =
                layout.projectDirectory.dir("src/desktopMain/assets")
            jvmArgs += $$"-splash:$APPDIR/resources/phovo_splash.png"
        }
    }
}
