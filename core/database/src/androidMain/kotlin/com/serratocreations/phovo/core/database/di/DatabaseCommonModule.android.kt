package com.serratocreations.phovo.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.serratocreations.phovo.core.database.PhovoDatabase
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
actual class DatabasePlatformModule actual constructor() {
    @Factory
    fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<PhovoDatabase> {
        val appContext = ctx.applicationContext
        val dbFile = appContext.getDatabasePath("my_room.db")
        return Room.databaseBuilder<PhovoDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}