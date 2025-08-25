package com.serratocreations.phovo.core.database.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.serratocreations.phovo.core.database.PhovoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module(includes = [DatabasePlatformModule::class])
@ComponentScan("com.serratocreations.phovo.core.database")
class DatabaseCommonModule {
    @Singleton
    fun phovoDatabase(
        builder: RoomDatabase.Builder<PhovoDatabase>
    ): PhovoDatabase {
        return builder
            //.addMigrations(MIGRATIONS)
            // TODO: Remove before production builds are made available
            .fallbackToDestructiveMigration(true)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @Singleton
    fun serverConfigDao(db: PhovoDatabase) = db.getServerConfigDao()

    @Singleton
    fun phovoItemDao(db: PhovoDatabase) = db.getPhovoItemDao()
}

@Module
expect class DatabasePlatformModule()