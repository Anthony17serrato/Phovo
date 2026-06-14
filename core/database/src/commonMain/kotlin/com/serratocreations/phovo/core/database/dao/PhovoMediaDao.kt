package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.serratocreations.phovo.core.database.entities.MediaItemMetadataEntity
import com.serratocreations.phovo.core.database.entities.LocalMediaEntity
import com.serratocreations.phovo.core.database.entities.LocalMediaItemWithMetadata
import com.serratocreations.phovo.core.database.entities.MediaItemWithMetadata
import com.serratocreations.phovo.core.database.entities.ProcessingMediaEntity
import com.serratocreations.phovo.core.database.entities.SyncLogEntity
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.model.MediaType
import kotlinx.coroutines.flow.Flow

private const val TAG = "PhovoMediaDao"

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

    @Query(
        """
        SELECT * 
        FROM LocalMediaEntity
        WHERE assetHash = :assetHash
            AND isPartial = FALSE
        LIMIT 1
    """
    )
    suspend fun getNonPartialLocalMediaByAssetHash(assetHash: String): LocalMediaEntity?

    @Transaction
    @Query(
        """
    SELECT 
        m.*, 
        l.assetHash AS local_assetHash,
        l.localUri AS local_localUri,
        l.isPartial AS local_isPartial
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

    @Query("""
        SELECT l.*
        FROM LocalMediaEntity l
        LEFT JOIN MediaItemMetadataEntity m
            ON l.assetHash = m.assetHash
        LEFT JOIN ProcessingMediaEntity p
            ON l.assetHash = p.assetHash
        WHERE m.assetHash IS NULL
        AND p.assetHash IS NULL
        AND l.isPartial = 0
        LIMIT 1
    """)
    fun observeFirstUnprocessedFullLocalMedia(): Flow<LocalMediaEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tryClaim(entity: ProcessingMediaEntity): Long

    @Query("""
    DELETE FROM ProcessingMediaEntity
    WHERE assetHash = :assetHash
    """)
    suspend fun removeClaim(assetHash: String): Int

    @Query(
        """
    SELECT 
        m.*,
        l.assetHash AS local_assetHash,
        l.localUri AS local_localUri,
        l.isPartial AS local_isPartial
    FROM MediaItemMetadataEntity m
    INNER JOIN LocalMediaEntity l
        ON m.assetHash = l.assetHash
    LEFT JOIN SyncLogEntity s
        ON m.assetHash = s.assetHash
    WHERE m.isSynced = FALSE
      AND m.mediaType = :mediaType
      AND (
          s.assetHash IS NULL 
          OR (
              s.isSyncInProgress = FALSE 
              AND s.syncFailedCount <= 3 
              AND (
                  s.lastSyncFailTimeUtc IS NULL 
                  OR s.lastSyncFailTimeUtc <= :currentTimeUtcMs - 300000 
                  OR s.lastSyncFailTimeUtc > :currentTimeUtcMs
              )
          )
      )
    ORDER BY m.timeStampUtcMs DESC
    LIMIT 1
    """
    )
    suspend fun getNextUnsyncedLocalItemExcludingSet(
        mediaType: MediaType,
        currentTimeUtcMs: Long
    ): LocalMediaItemWithMetadata?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItemToSyncLog(entity: SyncLogEntity): Long

    @Query(
        """
        UPDATE SyncLogEntity
        SET isSyncInProgress = TRUE
        WHERE assetHash = :assetHash AND isSyncInProgress = FALSE
        """
    )
    suspend fun updateSyncInProgressToTrue(assetHash: String): Int

    @Query("SELECT isSyncInProgress FROM SyncLogEntity WHERE assetHash = :assetHash LIMIT 1")
    suspend fun getSyncInProgressStatus(assetHash: String): Boolean?

    @Transaction
    suspend fun claimItemForSync(assetHash: String): Boolean {
        val updatedRows = updateSyncInProgressToTrue(assetHash)
        if (updatedRows > 0) {
            return true
        }
        val status = getSyncInProgressStatus(assetHash)
        if (status == null) {
            val insertResult = addItemToSyncLog(
                SyncLogEntity(
                    assetHash = assetHash,
                    isSyncInProgress = true,
                    syncError = null
                )
            )
            val isClaimed = insertResult != -1L
            if (isClaimed.not()) {
                PhovoLogger.withTag(TAG).i {
                    "claimItemForSync Failed to claim assetHash $assetHash, with syncInProgressStatus $status"
                }
            }
            return isClaimed
        }
        PhovoLogger.withTag(TAG).i {
            "claimItemForSync Failed to claim assetHash $assetHash, with syncInProgressStatus $status"
        }
        return false
    }

    @Query("""
    DELETE FROM SyncLogEntity
    WHERE assetHash = :assetHash
    """)
    suspend fun removeSyncAsset(assetHash: String)

    @Query(
        """
        UPDATE SyncLogEntity
        SET isSyncInProgress = FALSE,
            syncError = :syncError,
            syncFailedCount = syncFailedCount + 1,
            lastSyncFailTimeUtc = :lastSyncFailTimeUtc
        WHERE assetHash = :assetHash
        """
    )
    suspend fun incrementSyncFailedCount(
        assetHash: String,
        syncError: String?,
        lastSyncFailTimeUtc: Long
    ): Int

    @Transaction
    suspend fun addSyncFailure(
        assetHash: String,
        syncError: String?,
        lastSyncFailTimeUtc: Long
    ) {
        val updatedRows = incrementSyncFailedCount(assetHash, syncError, lastSyncFailTimeUtc)
        if (updatedRows == 0) {
            PhovoLogger.withTag(TAG).e {
                "addSyncFailure could not find existing sync log for assetHash $assetHash"
            }
            addItemToSyncLog(
                SyncLogEntity(
                    assetHash = assetHash,
                    isSyncInProgress = false,
                    syncError = syncError,
                    syncFailedCount = 1,
                    lastSyncFailTimeUtc = lastSyncFailTimeUtc
                )
            )
        }
    }

    @Query("""
    UPDATE SyncLogEntity
    SET isSyncInProgress = FALSE
    WHERE isSyncInProgress = TRUE
    """)
    suspend fun clearInProgressSyncLogs()

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
