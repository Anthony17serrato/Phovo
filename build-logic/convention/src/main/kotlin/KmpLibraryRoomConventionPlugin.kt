import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import com.android.build.api.dsl.LibraryExtension
import com.serratocreations.phovo.buildlogic.configureKmpRoom
import org.gradle.kotlin.dsl.getByType

class KmpLibraryRoomConventionPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            apply(plugin = "androidx.room")

            val extension = extensions.getByType<LibraryExtension>()
            configureKmpRoom(extension)
        }
    }
}