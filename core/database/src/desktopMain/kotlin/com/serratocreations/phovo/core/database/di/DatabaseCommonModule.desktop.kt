package com.serratocreations.phovo.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.serratocreations.phovo.core.common.util.AppDataDirectoryUtil
import com.serratocreations.phovo.core.database.PhovoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

internal actual fun getAndroidDesktopIosModule(): Module = module {
    factory<RoomDatabase.Builder<PhovoDatabase>> {
        val dbFile = File(AppDataDirectoryUtil.getAppDataDirectory("Phovo"), "phovo.db")
        Room.databaseBuilder<PhovoDatabase>(
            name = dbFile.absolutePath,
        )
    }
}