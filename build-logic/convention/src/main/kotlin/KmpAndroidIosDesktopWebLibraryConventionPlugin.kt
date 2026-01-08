import com.serratocreations.phovo.buildlogic.CustomSourceSets
import com.serratocreations.phovo.buildlogic.Targets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpAndroidIosDesktopWebLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // Re-use other build plugin
                apply("phovo.kmp.android.ios.desktop.library")
            }

            configureKotlinMultiplatform(
                isApplication = false,
                customSourceSets = setOf(CustomSourceSets.DesktopIosAndroid, CustomSourceSets.AndroidIosWeb),
                targetList = setOf(Targets.WEB)
            )
        }
    }
}