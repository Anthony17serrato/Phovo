import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.phovo.kmp.desktop.application)
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