package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serratocreations.phovo.core.database.entities.ClientConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClientConfigEntity)

    @Query("SELECT * FROM ClientConfigEntity LIMIT 1")
    fun clientConfigFlow(): Flow<ClientConfigEntity?>

    @Query("DELETE FROM ClientConfigEntity")
    suspend fun deleteConfig()
}
