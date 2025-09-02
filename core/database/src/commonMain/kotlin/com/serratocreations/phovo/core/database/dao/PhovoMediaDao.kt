package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serratocreations.phovo.core.database.entities.PhovoMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhovoMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PhovoMediaEntity)

    @Query("SELECT * FROM PhovoMediaEntity ORDER BY timeStampUtcMs DESC")
    fun observeAllDescendingTimestamp(): Flow<List<PhovoMediaEntity>>
}
