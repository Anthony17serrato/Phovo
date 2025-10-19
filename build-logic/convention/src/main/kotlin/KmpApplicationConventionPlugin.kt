import com.android.build.api.dsl.ApplicationExtension
import com.serratocreations.phovo.buildlogic.CustomSourceSets
import com.serratocreations.phovo.buildlogic.configureKotlinAndroid
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get

class KmpApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.multiplatform")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureKotlinMultiplatform(
                    customSourceSets = setOf(CustomSourceSets.DesktopIosAndroid, CustomSourceSets.IosAndroid, CustomSourceSets.AndroidIosWeb),
                    isApplication = true
                )
                sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
                sourceSets["main"].res.srcDirs("src/androidMain/res")
                sourceSets["main"].resources.srcDirs("src/commonMain/resources")
                defaultConfig {
                    targetSdk = 36
                    applicationId = "com.serratocreations.phovo"
                    versionCode = 1
                    versionName = "1.0"
                }
                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = false
                        // TODO Update when ready to release
                        signingConfig = signingConfigs.getByName("debug")
                    }
                }
                testOptions.animationsDisabled = true
            }
        }
    }
}