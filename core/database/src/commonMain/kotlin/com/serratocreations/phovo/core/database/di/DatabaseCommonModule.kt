package com.serratocreations.phovo.core.database.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.serratocreations.phovo.core.database.PhovoDatabase
import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun getAndroidDesktopIosModule(): Module

fun getDatabaseModule(): Module = module {
    includes(getAndroidDesktopIosModule())

    single<PhovoDatabase> {
        val builder: RoomDatabase.Builder<PhovoDatabase> = get()
        builder
            //.addMigrations(MIGRATIONS)
            // TODO: Remove before production builds are made available
            .fallbackToDestructiveMigration(true)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single<ServerConfigDao> {
        get<PhovoDatabase>().getServerConfigDao()
    }

    single<PhovoMediaDao> {
        get<PhovoDatabase>().getPhovoItemDao()
    }
}