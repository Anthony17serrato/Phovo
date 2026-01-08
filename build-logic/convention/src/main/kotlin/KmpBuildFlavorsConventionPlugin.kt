import com.serratocreations.phovo.buildlogic.configureBuildFlavors
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpBuildFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
            }
            configureBuildFlavors()
        }
    }
}