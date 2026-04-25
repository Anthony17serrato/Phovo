package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.serratocreations.phovo.core.database.entities.MediaItemMetadataEntity
import com.serratocreations.phovo.core.database.entities.LocalMediaEntity
import com.serratocreations.phovo.core.database.entities.LocalMediaItemWithMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemWithMetadata
import com.serratocreations.phovo.core.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface PhovoMediaDao {

    @Upsert
    suspend fun upsertMetadata(item: MediaItemMetadataEntity)

    @Upsert
    suspend fun upsertLocal(local: LocalMediaEntity)

    @Query(
        """
    UPDATE MediaItemMetadataEntity
    SET isSynced = TRUE
    WHERE assetHash = :assetHash
    """
    )
    suspend fun markAsSynced(assetHash: String)

    @Transaction
    suspend fun upsertMetadataWithLocalEntity(
        metadata: MediaItemMetadataEntity,
        local: LocalMediaEntity
    ) {
        upsertMetadata(metadata)
        upsertLocal(local)
    }

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: MediaItemMetadataEntity)

    @Transaction
    @Query("SELECT * FROM MediaItemMetadataEntity ORDER BY timeStampUtcMs DESC")
    fun observeAllDescendingTimestamp(): Flow<List<MediaItemWithMetadata>>

    @Query("SELECT COUNT(*) FROM MediaItemMetadataEntity WHERE isSynced = FALSE")
    fun observeUnsyncedMediaItemCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM MediaItemMetadataEntity WHERE assetHash = :assetHash LIMIT 1")
    suspend fun getMediaItemByAssetHash(
        assetHash: String
    ): MediaItemWithMetadata?

    @Query("SELECT * FROM LocalMediaEntity WHERE assetHash = :assetHash LIMIT 1")
    suspend fun getLocalMediaByAssetHash(assetHash: String): LocalMediaEntity?

    @Transaction
    @Query(
        """
    SELECT 
        m.*, 
        l.assetHash AS local_assetHash,
        l.localUri AS local_localUri
    FROM MediaItemMetadataEntity m
    INNER JOIN LocalMediaEntity l
        ON m.assetHash = l.assetHash
    WHERE m.assetHash = :assetHash
    LIMIT 1
    """
    )
    suspend fun getLocalMediaItemWithMetadataByAssetHash(
        assetHash: String
    ): LocalMediaItemWithMetadata?

    @Query(
        """
    SELECT 
        m.*,
        l.assetHash AS local_assetHash,
        l.localUri AS local_localUri
    FROM MediaItemMetadataEntity m
    INNER JOIN LocalMediaEntity l
        ON m.assetHash = l.assetHash
    WHERE m.isSynced = FALSE
      AND m.mediaType = :mediaType
      AND (:excludeNotEmpty OR m.assetHash NOT IN (:excludingHashes))
    ORDER BY m.timeStampUtcMs DESC
    LIMIT 1
    """
    )
    suspend fun getNextUnsyncedLocalItemExcludingSet(
        excludingHashes: Set<String>,
        mediaType: MediaType,
        excludeNotEmpty: Boolean
    ): LocalMediaItemWithMetadata?

    @Query("DELETE FROM MediaItemMetadataEntity")
    suspend fun clearAllMediaItems()

    @Query("DELETE FROM LocalMediaEntity")
    suspend fun clearAllMediaItemUris()

    // Deletes all records from both tables in a single transaction
    @Transaction
    suspend fun clearAllMediaData() {
        // It's usually good practice to delete the child/dependent table first
        // in case you ever add foreign key constraints later!
        clearAllMediaItemUris()
        clearAllMediaItems()
    }
}
