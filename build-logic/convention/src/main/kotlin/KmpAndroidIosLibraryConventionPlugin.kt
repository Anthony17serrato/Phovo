import com.serratocreations.phovo.buildlogic.Targets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpAndroidIosLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.multiplatform")
            }

            // Centralized configuration in configureKotlinMultiplatform
            configureKotlinMultiplatform(
                isUmbrella = false,
                targetList = setOf(Targets.ANDROID, Targets.IOS)
            )
        }
    }
}