import com.google.devtools.ksp.gradle.KspExtension
import com.serratocreations.phovo.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("org.jetbrains.kotlin.multiplatform")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.androidMain.dependencies {
                    implementation(libs.findLibrary("koin.android").get())
                }
                sourceSets.commonMain.dependencies {
                    // Koin
                    implementation(libs.findBundle("koin.common.kmp").get())
                    // Koin Annotations
                    api(libs.findLibrary("koin.annotations").get())
                }

                // KSP Common sourceSet https://insert-koin.io/docs/setup/annotations/
                sourceSets.named("commonMain").configure {
                    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
                }
            }

            // Trigger Common Metadata Generation from Native tasks
            project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
                if (name != "kspCommonMainKotlinMetadata") {
                    dependsOn("kspCommonMainKotlinMetadata")
                }
            }

            extensions.configure<KspExtension> {
                // Enable Koin Viewmodel Annotation
                arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
            }
        }
    }
}