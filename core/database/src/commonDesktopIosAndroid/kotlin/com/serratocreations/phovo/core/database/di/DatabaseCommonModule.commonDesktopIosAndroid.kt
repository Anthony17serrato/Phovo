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
actual class DatabaseCommonModule actual constructor() {
    @Singleton
    fun phovoDatabase(
        builder: RoomDatabase.Builder<PhovoDatabase>
    ): PhovoDatabase {
        return builder
            //.addMigrations(MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @Singleton
    fun serverConfigDao(db: PhovoDatabase) = db.getServerConfigDao()
}