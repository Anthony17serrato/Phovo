import com.serratocreations.phovo.buildlogic.CustomSourceSets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpUmbrellaLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.multiplatform")
            }

            configureKotlinMultiplatform(
                customSourceSets = setOf(
                    CustomSourceSets.DesktopIosAndroid,
                    CustomSourceSets.IosAndroid,
                    CustomSourceSets.AndroidIosWeb,
                    CustomSourceSets.AndroidDesktop
                ),
                isUmbrella = true,
                isApplication = false
            )
        }
    }
}