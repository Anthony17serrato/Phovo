import com.android.build.gradle.LibraryExtension
import com.serratocreations.phovo.buildlogic.Targets
import com.serratocreations.phovo.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KmpAndroidIosDesktopWebLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // Re-use other build plugin
                apply("phovo.kmp.android.ios.desktop.library")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinMultiplatform(
                    isApplication = false,
                    targetList = setOf(Targets.WASM)
                )
                // The resource prefix is derived from the module name,
                // so resources inside ":core:module1" must be prefixed with "core_module1_"
                resourcePrefix = path.split("""\W""".toRegex()).drop(1).distinct().joinToString(separator = "_").lowercase() + "_"
            }
        }
    }
}