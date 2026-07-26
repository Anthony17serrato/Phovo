import com.serratocreations.phovo.buildlogic.Targets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpWebApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("phovo.kmp.library.compose")
            }

            // Configure only the Web KMP target for applications
            configureKotlinMultiplatform(isApplication = true, isUmbrella = false, targetList = setOf(Targets.WEB))
        }
    }
}