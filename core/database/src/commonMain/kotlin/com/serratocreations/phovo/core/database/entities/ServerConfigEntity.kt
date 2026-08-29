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
    val backupDirectory: String,
    @ColumnInfo(name = "server_name")
    val serverName: String,
    /**
     * Stable identity for this server, generated on first configuration and preserved across
     * reconfiguration.
     * Clients persist it so they can re-find this server after its address changes.
     */
    @ColumnInfo(name = "server_id")
    val serverId: String
)