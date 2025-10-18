package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.serratocreations.phovo.core.database.entities.MediaItemEntity
import com.serratocreations.phovo.core.database.entities.MediaItemUriEntity
import com.serratocreations.phovo.core.database.entities.MediaItemWithUriEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhovoMediaDao {
    @Upsert
    suspend fun insert(item: MediaItemEntity, uri: MediaItemUriEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: MediaItemEntity)

    @Transaction
    @Query("SELECT * FROM MediaItemEntity ORDER BY timeStampUtcMs DESC")
    fun observeAllDescendingTimestamp(): Flow<List<MediaItemWithUriEntity>>

    @Transaction
    @Query("SELECT * FROM MediaItemEntity WHERE remoteUuid IS NULL")
    fun observeAllUnsyncedMediaItems(): Flow<List<MediaItemWithUriEntity>>

    @Transaction
    @Query("SELECT * FROM MediaItemEntity WHERE localUuid IS :uuid LIMIT 1")
    suspend fun getMediaItemByLocalUuid(uuid: String): MediaItemWithUriEntity?
}
