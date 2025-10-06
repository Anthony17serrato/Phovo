package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serratocreations.phovo.core.database.entities.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhovoMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItemEntity)

    @Query("SELECT * FROM MediaItemEntity ORDER BY timeStampUtcMs DESC")
    fun observeAllDescendingTimestamp(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM MediaItemEntity WHERE remoteUri IS NULL")
    fun observeAllUnsyncedMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM MediaItemEntity WHERE localUuid IS :uuid LIMIT 1")
    suspend fun getMediaItemByLocalUuid(uuid: String): MediaItemEntity?
}
