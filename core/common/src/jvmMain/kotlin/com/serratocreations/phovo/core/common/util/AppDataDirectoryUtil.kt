package com.serratocreations.phovo.core.common.util

import java.io.File

object AppDataDirectoryUtil {
    fun getAppDataDirectory(appName: String): File {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")

        return when {
            os.contains("win") -> File(System.getenv("APPDATA"), appName)
            os.contains("mac") -> File(userHome, "Library/Application Support/$appName")
            else -> File(userHome, ".local/share/$appName")
        }.apply { mkdirs() }
    }
}