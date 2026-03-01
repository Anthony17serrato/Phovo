import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.phovo.kmp.application.koin)
    alias(libs.plugins.phovo.kmp.application.compose)
    alias(libs.plugins.phovo.kmp.application.application)
    alias(libs.plugins.serialization)
    alias(libs.plugins.phovo.kmp.build.flavors)
}

kotlin {
    sourceSets {
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
            implementation(projects.core.navigation)

            implementation(libs.compose.resources)
            implementation(libs.serialization.json)
            implementation(libs.platformtools.darkmodedetector)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.bundles.navigation3)
        }
        jvmMain.dependencies {
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
                layout.projectDirectory.dir("src/jvmMain/assets")
            jvmArgs += $$"-splash:$APPDIR/resources/phovo_splash.png"
        }
    }
}
