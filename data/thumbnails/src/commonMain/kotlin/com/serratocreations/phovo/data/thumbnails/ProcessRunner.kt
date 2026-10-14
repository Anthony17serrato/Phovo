package com.serratocreations.phovo.data.thumbnails

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.time.Duration

/** What a process printed to standard output, and how it exited. */
internal class ProcessOutput(val exitCode: Int, val stdout: String)

/**
 * Runs [command] and returns its output, or null when it does not exit within [timeout]. Standard
 * error goes to this process's own.
 *
 * The process is killed when it times out and when the caller is cancelled, so a process that hangs
 * cannot outlive the work that started it.
 *
 * @throws IOException when the process cannot be started.
 */
internal suspend fun runProcess(
    command: List<String>,
    timeout: Duration,
    ioDispatcher: CoroutineDispatcher
): ProcessOutput? = withContext(ioDispatcher) {
    val process = ProcessBuilder(command)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
    try {
        // Reading blocks until the process closes its output, so it runs alongside the wait below
        // instead of before it. It also has to keep running, or a process with more output than
        // the pipe holds would block writing and never exit.
        val stdout = async {
            try {
                process.inputStream.bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                // The stream closes under the read when the process is killed.
                null
            }
        }
        val exited = withTimeoutOrNull(timeout) { process.onExit().await() }
        if (exited == null) return@withContext null
        ProcessOutput(exitCode = process.exitValue(), stdout = stdout.await().orEmpty())
    } finally {
        if (process.isAlive) process.destroyForcibly()
    }
}
