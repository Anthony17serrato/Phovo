package com.serratocreations.phovo.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.serratocreations.phovo.core.database.PhovoDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual fun getAndroidDesktopIosModule(): Module = module {
    factory<RoomDatabase.Builder<PhovoDatabase>> {
        fun documentDirectory(): String {
            val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            return requireNotNull(documentDirectory?.path)
        }

        val dbFilePath = documentDirectory() + "/phovo.db"
        Room.databaseBuilder<PhovoDatabase>(
            name = dbFilePath,
        )
    }
}