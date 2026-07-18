import com.serratocreations.phovo.buildlogic.CustomSourceSets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpAndroidIosDesktopLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.multiplatform")
                // Re-use more refined target configurations
                apply("phovo.kmp.desktop.library")
                apply("phovo.kmp.android.ios.library")
            }

            // Centralized configuration in configureKotlinMultiplatform
            configureKotlinMultiplatform(
                isUmbrella = false,
                customSourceSets = setOf(
                    CustomSourceSets.IosAndroid,
                    CustomSourceSets.AndroidDesktop
                ),
                // All targets configured by refined plugins
                targetList = emptySet()
            )
        }
    }
}