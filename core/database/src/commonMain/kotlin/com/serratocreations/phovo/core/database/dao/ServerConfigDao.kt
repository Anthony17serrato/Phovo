package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serratocreations.phovo.core.database.entities.ServerConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ServerConfigEntity)

    @Query("SELECT * FROM ServerConfigEntity LIMIT 1")
    fun serverConfigFlow(): Flow<ServerConfigEntity?>
}