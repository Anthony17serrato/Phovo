package com.serratocreations.phovo.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.ServerConfigEntity
import com.serratocreations.phovo.core.database.entities.MediaItemEntity

@Database(entities = [ServerConfigEntity::class, MediaItemEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class PhovoDatabase : RoomDatabase() {
    abstract fun getServerConfigDao(): ServerConfigDao
    abstract fun getPhovoItemDao(): PhovoMediaDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<PhovoDatabase> {
    override fun initialize(): PhovoDatabase
}