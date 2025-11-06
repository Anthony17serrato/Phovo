import com.android.build.gradle.LibraryExtension
import com.serratocreations.phovo.buildlogic.CustomSourceSets
import com.serratocreations.phovo.buildlogic.Targets
import com.serratocreations.phovo.buildlogic.configureKotlinAndroid
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KmpAndroidIosDesktopLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.multiplatform")
                // Re-use desktop config
                apply("phovo.kmp.desktop.library")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                configureKotlinMultiplatform(
                    isApplication = false,
                    customSourceSets = setOf(
                        CustomSourceSets.IosAndroid,
                        CustomSourceSets.AndroidDesktop
                    ),
                    targetList = setOf(Targets.ANDROID, Targets.IOS)
                )
                defaultConfig.targetSdk = 34
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true
                // The resource prefix is derived from the module name,
                // so resources inside ":core:module1" must be prefixed with "core_module1_"
                resourcePrefix = path.split("""\W""".toRegex()).drop(1).distinct().joinToString(separator = "_").lowercase() + "_"
            }
        }
    }
}