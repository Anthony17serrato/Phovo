package com.serratocreations.phovo.core.logger

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Clock

internal expect fun platformFileLogWriter(): PlatformFileLogWriter

internal abstract class PlatformFileLogWriter(
    scope: CoroutineScope
): LogWriter() {
    private val logQueue = Channel<LogEntry>(Channel.UNLIMITED)

    init {
        scope.launch {
            for(logEntry in logQueue) {
                writeToFile(logEntry)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val currentInstant = Clock.System.now()
        val datetime: LocalDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val timestamp = datetime.toString() // ISO 8601 format
        val logMessage = "$timestamp | ${severity.name} | $tag | $message"
        // trySend guaranteed on unlimited channel
        logQueue.trySend(
            LogEntry(logMessage, throwable)
        )
    }

    abstract fun writeToFile(logEntry: LogEntry)
}

internal class NoOpFileWriter(
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : PlatformFileLogWriter(CoroutineScope(SupervisorJob() + ioDispatcher)) {
    override fun writeToFile(logEntry: LogEntry) {
        // No - Op
    }
}

data class LogEntry(
    val logMessage: String,
    val throwable: Throwable?
)

