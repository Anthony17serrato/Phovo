package com.serratocreations.phovo.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import com.serratocreations.phovo.core.database.entities.ServerConfigEntity

@Database(entities = [ServerConfigEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class PhovoDatabase : RoomDatabase() {
    abstract fun getServerConfigDao(): ServerConfigDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<PhovoDatabase> {
    override fun initialize(): PhovoDatabase
}