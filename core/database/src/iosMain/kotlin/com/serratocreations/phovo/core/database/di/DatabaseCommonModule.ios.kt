package com.serratocreations.phovo.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.serratocreations.phovo.core.database.PhovoDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@Module
@ComponentScan("com.serratocreations.phovo.core.database")
actual class DatabasePlatformModule actual constructor() {
    @OptIn(ExperimentalForeignApi::class)
    @Factory
    fun getDatabaseBuilder(): RoomDatabase.Builder<PhovoDatabase> {
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
        return Room.databaseBuilder<PhovoDatabase>(
            name = dbFilePath,
        )
    }
}