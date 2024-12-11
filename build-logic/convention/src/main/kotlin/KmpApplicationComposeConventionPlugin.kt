import com.android.build.api.dsl.ApplicationExtension
import com.serratocreations.phovo.buildlogic.configureKmpCompose
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.api.Plugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

class KmpApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "org.jetbrains.compose")

            val extension = extensions.getByType<ApplicationExtension>()
            configureKmpCompose(extension)
            extensions.configure<ComposeExtension> {
                configure<DesktopExtension> {
                    application {
                        mainClass = "com.serratocreations.phovo.MainKt"

                        nativeDistributions {
                            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                            packageName = "com.serratocreations.phovo"
                            packageVersion = "1.0.0"
                        }
                    }
                }
            }
        }
    }
}