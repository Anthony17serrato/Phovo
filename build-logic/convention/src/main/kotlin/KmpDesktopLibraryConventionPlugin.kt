import com.serratocreations.phovo.buildlogic.Targets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpDesktopLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
            }

            // Configure only the Desktop (JVM) KMP target for libraries
            configureKotlinMultiplatform(isApplication = false, targetList = setOf(Targets.DESKTOP))
        }
    }
}
