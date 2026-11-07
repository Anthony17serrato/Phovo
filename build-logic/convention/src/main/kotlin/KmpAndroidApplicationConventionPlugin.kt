import com.android.build.api.dsl.ApplicationExtension
import com.serratocreations.phovo.buildlogic.configureAndroidApplication
import com.serratocreations.phovo.buildlogic.phovoVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KmpAndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("phovo.kmp.library.compose")
            }

            extensions.configure<ApplicationExtension> {
                configureAndroidApplication()
                defaultConfig {
                    targetSdk = 37
                    applicationId = "com.serratocreations.phovo"
                    val version = phovoVersion()
                    versionCode = version.versionCode
                    versionName = version.name
                }
                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
                buildTypes {
                    // TODO temporary config
                    getByName("release") {
                        //isDebuggable = true
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
