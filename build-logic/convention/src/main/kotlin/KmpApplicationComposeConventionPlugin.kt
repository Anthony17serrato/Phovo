import com.android.build.api.dsl.ApplicationExtension
import com.serratocreations.phovo.buildlogic.configureKmpCompose
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class KmpApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "org.jetbrains.compose")

            val extension = extensions.getByType<ApplicationExtension>()
            configureKmpCompose(extension)
        }
    }
}