package com.serratocreations.phovo.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.serratocreations.phovo.core.database.converters.AssetLocationConverter
import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import com.serratocreations.phovo.core.database.dao.PhovoMediaDao
import com.serratocreations.phovo.core.database.entities.ServerConfigEntity
import com.serratocreations.phovo.core.database.entities.MediaItemMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemLocationEntity

@Database(entities = [ServerConfigEntity::class, MediaItemMetadata::class, MediaItemLocationEntity::class], version = 1)
@TypeConverters(AssetLocationConverter::class)
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