package com.serratocreations.phovo.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ClientConfigEntity(
    // Since only one server URL is active at a time, we use a single row
    // with a fixed primary key of 1 and an onConflict REPLACE strategy.
    @PrimaryKey val id: Long = 1,
    val serverUrl: String,
    /**
     * Identity of the paired server. Both pairing routes establish it: discovery reads it from the
     * TXT record, and a typed address is probed before it is accepted.
     */
    @ColumnInfo(name = "server_id")
    val serverId: String,
    /**
     * Last name the server reported for itself. A cache, not configuration: it starts as the mDNS
     * service name seen while pairing and is overwritten by every health probe, since the server is
     * the only authority on what it is called and the user can rename it at any time.
     */
    @ColumnInfo(name = "server_name")
    val serverName: String
)
