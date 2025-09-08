package com.serratocreations.phovo.core.logger

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal actual fun platformFileLogWriter(): PlatformFileLogWriter = AndroidFileLogWriter()

internal class AndroidFileLogWriter(
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PlatformFileLogWriter(CoroutineScope(SupervisorJob() + ioDispatcher)), KoinComponent {
    private val context: Context by inject()

    @OptIn(ExperimentalTime::class)
    private val logFile: File
        get() {
            val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val logFileName = "phovo_logs_android${today}.log"
            return File(context.filesDir, logFileName).apply {
                if (!exists()) {
                    createNewFile()
                }
            }
        }

    override fun writeToFile(logEntry: LogEntry) {
        try {
            FileOutputStream(logFile, true).bufferedWriter().use { writer ->
                writer.write(logEntry.logMessage)
                writer.newLine()
                logEntry.throwable?.let {
                    writer.write(it.stackTraceToString())
                    writer.newLine()
                }
            }
        } catch (e: Exception) {
            // Log to the Android logcat without using Kermit
            Log.e("AndroidFileLogWriter", "Error writing to log file", e)
        }
    }
}