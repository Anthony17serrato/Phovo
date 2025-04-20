package com.serratocreations.phovo.core.logger

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.io.File
import java.io.FileOutputStream

internal actual fun platformFileLogWriter(): PlatformFileLogWriter = DesktopFileLogWriter()

internal class DesktopFileLogWriter(
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PlatformFileLogWriter(CoroutineScope(SupervisorJob() + ioDispatcher)) {

    private val logDirectory = try {
        val appName = "Phovo"
        val osName = System.getProperty("os.name").lowercase()
        val directory = when {
            osName.contains("win") -> {
                val appData = System.getenv("LOCALAPPDATA") ?: System.getenv("APPDATA")
                File(appData, appName)
            }
            osName.contains("mac") || osName.contains("darwin") -> {
                File(System.getProperty("user.home"), "Library/Application Support/$appName")
            }
            else -> { // Linux or other
                File(System.getProperty("user.home"), ".local/share/$appName")
            }
        }
        if (directory.exists().not()) {
            directory.mkdirs() // Creates the directory and any necessary parent directories
        }
        directory
    } catch(e: Exception) {
        // File could not be created, no point in logging
        null
    }

    private val logFile: File?
        get() {
            val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val logFileName = "phovo_logs_desktop_${today}.log"
            if (logDirectory == null) return null
            val file = try {
                File(logDirectory, logFileName).apply {
                    if (!exists()) {
                        createNewFile()
                    }
                }
            } catch (e: Exception) {
                // File could not be created, no point in logging
                null
            }
            return file
        }

    override fun writeToFile(logEntry: LogEntry) {
        try {
            println(logFile)
            logFile?.let { logFileNotNull ->
                FileOutputStream(logFileNotNull, true).bufferedWriter().use { writer ->
                    writer.write(logEntry.logMessage)
                    writer.newLine()
                    logEntry.throwable?.let {
                        writer.write(it.stackTraceToString())
                        writer.newLine()
                    }
                }
            }
        } catch (e: Exception) {
            // In a desktop environment, you might want to print to the console or use a different logging mechanism for errors.
            println("Error writing to log file: ${e.message}")
            e.printStackTrace()
        }
    }
}