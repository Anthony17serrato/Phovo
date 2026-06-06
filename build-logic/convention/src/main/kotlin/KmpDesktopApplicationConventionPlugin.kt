import com.serratocreations.phovo.buildlogic.Targets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

class KmpDesktopApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("phovo.kmp.library.compose")
            }

            // Configure only the Desktop (JVM) KMP target for libraries
            configureKotlinMultiplatform(isApplication = true, isUmbrella = false, targetList = setOf(Targets.DESKTOP))
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