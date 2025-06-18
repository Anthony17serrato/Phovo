package com.serratocreations.phovo.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.serratocreations.phovo.core.common.util.AppDataDirectoryUtil
import com.serratocreations.phovo.core.database.PhovoDatabase
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import java.io.File

@Module
actual class DatabasePlatformModule actual constructor() {
    @Factory
    fun getDatabaseBuilder(): RoomDatabase.Builder<PhovoDatabase> {
        val dbFile = File(AppDataDirectoryUtil.getAppDataDirectory("Phovo"), "phovo.db")
        return Room.databaseBuilder<PhovoDatabase>(
            name = dbFile.absolutePath,
        )
    }
}