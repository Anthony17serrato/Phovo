package com.serratocreations.phovo.data.thumbnails

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BundledFfmpegToolsTest {

    private val scope = CoroutineScope(SupervisorJob())
    private val installDirectory = Files.createTempDirectory("phovo-ffmpeg-test").toFile()

    @AfterTest
    fun tearDown() {
        scope.cancel()
        installDirectory.deleteRecursively()
    }

    private fun tools() = BundledFfmpegTools(scope, Dispatchers.IO, installDirectory)

    @Test
    fun firstUse_installsUnderTheToolAndItsChecksum() = runBlocking {
        val ffprobe = tools().ffprobe.await().file

        assertTrue(ffprobe.isFile)
        assertTrue(ffprobe.canExecute())
        assertEquals(File(installDirectory, "ffprobe"), ffprobe.parentFile.parentFile)
        assertTrue(ffprobe.parentFile.name.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun laterLaunches_reuseTheInstalledTool() = runBlocking {
        val installed = tools().ffprobe.await().file
        val modified = installed.lastModified() - 10_000
        installed.setLastModified(modified)

        val reused = tools().ffprobe.await().file

        assertEquals(installed, reused)
        assertEquals(modified, reused.lastModified(), "the installed tool was written again")
        assertEquals(listOf(installed.name), installed.parentFile.list()?.toList())
    }

    @Test
    fun olderVersions_areDeleted_otherToolsAreKept() = runBlocking {
        val oldVersion = File(installDirectory, "ffprobe/${"0".repeat(64)}").apply { mkdirs() }
        File(oldVersion, "ffprobe").writeText("old")
        val ffmpegInstall = File(installDirectory, "ffmpeg/${"1".repeat(64)}").apply { mkdirs() }

        tools().ffprobe.await()

        assertFalse(oldVersion.exists())
        assertTrue(ffmpegInstall.exists())
    }

    @Test
    fun leftoverPartialCopy_isNotUsed() = runBlocking {
        val installed = tools().ffprobe.await().file
        val versionDirectory = installed.parentFile
        installed.delete()
        File(versionDirectory, "ffprobe.123.part").writeText("truncated")

        val reinstalled = tools().ffprobe.await().file

        assertEquals(installed, reinstalled)
        assertTrue(reinstalled.length() > 1_000_000, "the tool was not copied in full")
    }
}
