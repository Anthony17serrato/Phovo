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

    /**
     * Refreshes only the cached name. A full insert would have to carry the url and identity along
     * with it, so a health probe landing next to a pairing write could put back an address the user
     * has already moved away from.
     */
    @Query("UPDATE ClientConfigEntity SET server_name = :serverName WHERE id = 1")
    suspend fun updateServerName(serverName: String)

    @Query("SELECT * FROM ClientConfigEntity LIMIT 1")
    fun clientConfigFlow(): Flow<ClientConfigEntity?>

    @Query("DELETE FROM ClientConfigEntity")
    suspend fun deleteConfig()
}
