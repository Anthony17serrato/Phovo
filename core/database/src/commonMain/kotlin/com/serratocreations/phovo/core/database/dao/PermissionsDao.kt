package com.serratocreations.phovo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serratocreations.phovo.core.database.entities.PermissionsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PermissionsEntity)

    @Query("SELECT * FROM PermissionsEntity")
    fun permissionFlow(): Flow<List<PermissionsEntity>>

    @Query("DELETE FROM PermissionsEntity WHERE permissionId = :permissionId")
    suspend fun remove(permissionId: String)
}