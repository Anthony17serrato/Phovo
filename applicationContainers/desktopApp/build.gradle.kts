import com.serratocreations.phovo.buildlogic.phovoVersion
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
            implementation(projects.core.designsystem)
            implementation(libs.compose.resources)
            implementation(libs.platformtools.darkmodedetector)
        }
    }
}

compose.resources {
    packageOfResClass = "com.serratocreations.phovo.desktop.resources"
}

compose.desktop {
    application {
        mainClass = "com.serratocreations.phovo.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // The user-facing app name: Phovo.app, the Start menu entry, the Linux launcher.
            packageName = "Phovo"
            // Numeric only: calculated from version.properties (1.0.0-alpha.1 → 1.0.11).
            packageVersion = phovoVersion().desktopPackageVersion

            val icons = layout.projectDirectory.dir("icons")
            macOS {
                // Matches the Android applicationId. Kept distinct from the iOS app
                // (com.serratocreations.phovo.Phovo) so the iPad app on Apple Silicon Macs can't
                // collide with this one. Freeze it after 1.0: macOS ties preferences, Keychain
                // items and privacy permissions to it.
                bundleID = "com.serratocreations.phovo"
                dockName = "Phovo"
                iconFile.set(icons.file("phovo.icns"))
            }
            windows {
                menuGroup = "Phovo"
                shortcut = true
                dirChooser = true
                // Never change this: MSI uses it to treat new versions as upgrades of the old one.
                upgradeUuid = "65deda65-dc43-439b-bb5a-aa1f56dfc40d"
                iconFile.set(icons.file("phovo.ico"))
            }
            linux {
                modules("jdk.security.auth")
                // Debian package names must be lowercase.
                packageName = "phovo"
                shortcut = true
                // Debian control Section: only; lowercase per Debian policy.
                appCategory = "graphics"
                // Becomes Categories= in the generated .desktop file.
                menuGroup = "Graphics;Photography;"
                iconFile.set(icons.file("phovo.png"))
            }
            appResourcesRootDir =
                layout.projectDirectory.dir("src/jvmMain/assets")
            jvmArgs += $$"-splash:$APPDIR/resources/phovo_splash.png"
        }
    }
}