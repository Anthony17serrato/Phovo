package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serratocreations.phovo.core.database.entities.PhovoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhovoItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PhovoItemEntity)

    @Query("SELECT * FROM PhovoItemEntity ORDER BY timeStampUtcMs DESC")
    fun observeAll(): Flow<List<PhovoItemEntity>>
}
