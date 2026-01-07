import com.serratocreations.phovo.buildlogic.CustomSourceSets
import com.serratocreations.phovo.buildlogic.Targets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpAndroidIosDesktopLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.multiplatform")
                // Re-use desktop config
                apply("phovo.kmp.desktop.library")
            }

            configureKotlinMultiplatform(
                isApplication = false,
                customSourceSets = setOf(
                    CustomSourceSets.IosAndroid,
                    CustomSourceSets.AndroidDesktop
                ),
                targetList = setOf(Targets.ANDROID, Targets.IOS)
            )
        }
    }
}