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

    @Query("SELECT * FROM ClientConfigEntity LIMIT 1")
    suspend fun getClientConfig(): ClientConfigEntity?

    /**
     * Updates only the cached address, leaving identity untouched. Kept separate from [insert] so
     * that re-resolving an address cannot accidentally rewrite which server we are paired with.
     */
    @Query("UPDATE ClientConfigEntity SET serverUrl = :serverUrl WHERE id = 1")
    suspend fun updateServerUrl(serverUrl: String)

    /**
     * Records the identity a server reported for a pairing made from a manually entered address,
     * which starts with no identity. Without this such a pairing could never be re-resolved after
     * the server's address changed.
     */
    @Query("UPDATE ClientConfigEntity SET server_id = :serverId WHERE id = 1 AND server_id IS NULL")
    suspend fun adoptServerId(serverId: String)

    @Query("DELETE FROM ClientConfigEntity")
    suspend fun deleteConfig()
}
