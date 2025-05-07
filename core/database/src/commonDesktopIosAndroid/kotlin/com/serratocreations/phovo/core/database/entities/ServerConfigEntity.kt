package com.serratocreations.phovo.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ServerConfigEntity(
    // Only one row can exist in the table so primary key is always 1 with an insert
    // strategy of REPLACE.
    @PrimaryKey val id: Long = 1,
    @ColumnInfo(name = "backup_directory")
    val backupDirectory: String
)