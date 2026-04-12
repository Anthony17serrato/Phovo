package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.serratocreations.phovo.core.database.entities.LOCAL_SERIAL_ID
import com.serratocreations.phovo.core.database.entities.MediaItemMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemLocationEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithMetadata
import com.serratocreations.phovo.core.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface PhovoMediaDao {
    // Already in a transaction as per transaction documentation
    @Upsert
    suspend fun insert(item: MediaItemMetadata, uri: MediaItemLocationEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: MediaItemMetadata)

    @Query(
        """
    SELECT 
        MediaItemMetadata.*,
        MediaItemLocationEntity.assetHash AS uri_assetHash,
        MediaItemLocationEntity.assetLocation AS uri_assetLocation,
        MediaItemLocationEntity.uri AS uri_uri
    FROM MediaItemMetadata
    INNER JOIN MediaItemLocationEntity
        ON MediaItemMetadata.metadataAssetHash = MediaItemLocationEntity.assetHash
    ORDER BY MediaItemMetadata.timeStampUtcMs DESC
    """
    )
    fun observeAllDescendingTimestamp(): Flow<List<MediaItemWithMetadata>>

    // 0 indicates Local Asset as per AssetLocation.Local
    @Query("SELECT COUNT(*) FROM MediaItemLocationEntity WHERE assetLocation == $LOCAL_SERIAL_ID")
    fun observeUnsyncedMediaItemCount(): Flow<Int>

    @Query(
        """
    SELECT 
        MediaItemMetadata.*,
        MediaItemLocationEntity.assetHash AS uri_assetHash,
        MediaItemLocationEntity.assetLocation AS uri_assetLocation,
        MediaItemLocationEntity.uri AS uri_uri
    FROM MediaItemMetadata
    INNER JOIN MediaItemLocationEntity
        ON MediaItemMetadata.metadataAssetHash = MediaItemLocationEntity.assetHash
    WHERE MediaItemMetadata.metadataAssetHash = :uuid
    LIMIT 1
    """
    )
    suspend fun getMediaItemByLocalUuid(
        uuid: String
    ): MediaItemWithMetadata?

    @Query(
        """
    SELECT 
        MediaItemMetadata.*,
        MediaItemLocationEntity.assetHash AS uri_assetHash,
        MediaItemLocationEntity.assetLocation AS uri_assetLocation,
        MediaItemLocationEntity.uri AS uri_uri
    FROM MediaItemMetadata
    INNER JOIN MediaItemLocationEntity
        ON MediaItemMetadata.metadataAssetHash = MediaItemLocationEntity.assetHash
    WHERE MediaItemMetadata.mediaType = :mediaType
      AND (:excludeNotEmpty OR MediaItemMetadata.metadataAssetHash NOT IN (:excludingHashes))
      AND MediaItemLocationEntity.assetLocation = $LOCAL_SERIAL_ID
    ORDER BY MediaItemMetadata.timeStampUtcMs DESC
    LIMIT 1
    """
    )
    suspend fun getNextUnsyncedItemExcludingUuidSet(
        excludingHashes: Set<String>,
        mediaType: MediaType,
        excludeNotEmpty: Boolean
    ): MediaItemWithMetadata?

    @Query("DELETE FROM MediaItemMetadata")
    suspend fun clearAllMediaItems()

    @Query("DELETE FROM MediaItemLocationEntity")
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
