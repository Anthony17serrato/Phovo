package com.serratocreations.phovo.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.serratocreations.phovo.core.database.PhovoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosModule(): Module = module {
    factory<RoomDatabase.Builder<PhovoDatabase>> {
        val appContext = get<Context>().applicationContext
        val dbFile = appContext.getDatabasePath("phovo.db")
        Room.databaseBuilder<PhovoDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}