import com.serratocreations.phovo.buildlogic.CustomSourceSets
import com.serratocreations.phovo.buildlogic.Targets
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
                // Commented source sets are not needed if Web is not supported
                customSourceSets = setOf(
                    //CustomSourceSets.DesktopIosAndroid,
                    CustomSourceSets.IosAndroid,
                    //CustomSourceSets.AndroidIosWeb,
                    CustomSourceSets.AndroidDesktop
                ),
                targetList = setOf(Targets.DESKTOP, Targets.IOS, Targets.ANDROID),
                isUmbrella = true,
                isApplication = false
            )
        }
    }
}