import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import com.serratocreations.phovo.buildlogic.configureKmpRoom

class KmpLibraryRoomConventionPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            apply(plugin = "com.google.devtools.ksp")
            apply(plugin = "androidx.room")

            configureKmpRoom()
        }
    }
}