package com.serratocreations.phovo.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Lets a dependency on a `t64` package also be met by its pre-t64 name: `libasound2t64` becomes
 * `libasound2t64 | libasound2`.
 *
 * jpackage names a DEB's dependencies after the packages on the machine that builds it. On Ubuntu
 * 24.04 those are the 64-bit time_t names (`libasound2t64`, `libpng16-16t64`) that only Debian 13 and
 * Ubuntu 24.04+ have, so Debian 12, Raspberry Pi OS Bookworm, Ubuntu 22.04 and Mint 21 refuse the
 * package even though the libraries are installed. The `|` alternative accepts either name.
 *
 * [depends] is the value of a control file's `Depends:` field. Entries keep any version constraint,
 * and entries that already list alternatives are left as they are.
 */
fun widenT64Dependencies(depends: String): String =
    depends.split(",").map(String::trim).filter(String::isNotEmpty).joinToString(", ") { entry ->
        val name = entry.substringBefore(" ").substringBefore("(")
        val constraint = entry.removePrefix(name)
        if ("|" in entry || !name.endsWith("t64")) entry else "$entry | ${name.removeSuffix("t64")}$constraint"
    }

/**
 * Rewrites the DEB that `packageDeb` built so its `t64` dependencies also accept their old names (see
 * [widenT64Dependencies]). Needs `dpkg-deb`, so it only runs on Linux.
 */
abstract class WidenDebDependenciesTask : DefaultTask() {
    /** packageDeb's output folder. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val debDirectory: DirectoryProperty

    /** Picks the current build's DEB out of [debDirectory], e.g. 1.0.11. */
    @get:Input
    abstract val packageVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject abstract val exec: ExecOperations
    @get:Inject abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun widen() {
        val version = packageVersion.get()
        val debs = debDirectory.get().asFile.listFiles { file -> file.name.endsWith(".deb") && version in file.name }
            .orEmpty()
        val deb = debs.singleOrNull()
            ?: throw GradleException("Expected one $version .deb in ${debDirectory.get()}, found ${debs.map { it.name }}.")

        fileSystem.delete { delete(outputDirectory) }
        val output = outputDirectory.get().asFile.apply { mkdirs() }
        val root = temporaryDir.resolve("root").apply { deleteRecursively() }

        exec.exec { commandLine("dpkg-deb", "--raw-extract", deb.absolutePath, root.absolutePath) }
        val control = root.resolve("DEBIAN/control")
        val lines = control.readLines()
        val dependsIndex = lines.indexOfFirst { it.startsWith("Depends:") }
        if (dependsIndex >= 0) {
            val widened = widenT64Dependencies(lines[dependsIndex].removePrefix("Depends:"))
            control.writeText(
                lines.toMutableList().apply { set(dependsIndex, "Depends: $widened") }.joinToString("\n") + "\n"
            )
            logger.lifecycle("${deb.name} Depends: $widened")
        }
        // --root-owner-group: the files were extracted as the build user, but must install as root.
        exec.exec {
            commandLine("dpkg-deb", "--root-owner-group", "--build", root.absolutePath, output.resolve(deb.name).absolutePath)
        }
    }
}
