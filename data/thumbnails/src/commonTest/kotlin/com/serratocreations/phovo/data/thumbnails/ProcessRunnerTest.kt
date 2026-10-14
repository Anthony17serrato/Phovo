package com.serratocreations.phovo.data.thumbnails

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ProcessRunnerTest {

    private val directory = Files.createTempDirectory("phovo-process-test").toFile()

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun finishedProcess_returnsItsExitCodeAndOutput() = runBlocking {
        val result = runProcess(listOf("sh", "-c", "printf hello; exit 3"), 10.seconds, Dispatchers.IO)

        assertEquals(3, result?.exitCode)
        assertEquals("hello", result?.stdout)
    }

    @Test
    fun outputLargerThanThePipe_isReadInFull() = runBlocking {
        val result = runProcess(
            listOf("sh", "-c", "head -c 500000 /dev/zero | tr '\\000' a"),
            10.seconds,
            Dispatchers.IO
        )

        assertEquals(500_000, result?.stdout?.length)
    }

    @Test
    fun hangingProcess_timesOut_andIsKilled() = runBlocking {
        val pidFile = File(directory, "pid")
        val started = TimeSource.Monotonic.markNow()

        val result = runProcess(hangingCommand(pidFile), 300.milliseconds, Dispatchers.IO)

        assertNull(result)
        assertTrue(started.elapsedNow() < 10.seconds, "the timeout did not end the wait")
        assertProcessEnded(pidFile)
    }

    @Test
    fun cancellation_killsTheProcess() = runBlocking {
        val pidFile = File(directory, "pid")
        val job = launch { runProcess(hangingCommand(pidFile), 60.seconds, Dispatchers.IO) }
        while (!pidFile.exists() || pidFile.readText().isBlank()) delay(20)

        job.cancel()
        job.join()

        assertProcessEnded(pidFile)
    }

    /** Writes its PID to [pidFile], then becomes a process that sleeps far longer than any test. */
    private fun hangingCommand(pidFile: File) =
        listOf("sh", "-c", "echo $$ > \"$1\"; exec sleep 60", "sh", pidFile.absolutePath)

    private fun assertProcessEnded(pidFile: File) {
        val pid = pidFile.readText().trim().toLong()
        val handle = ProcessHandle.of(pid)
        if (handle.isPresent) {
            runCatching { handle.get().onExit().get(5, TimeUnit.SECONDS) }
            assertFalse(handle.get().isAlive, "process $pid is still running")
        }
    }
}
