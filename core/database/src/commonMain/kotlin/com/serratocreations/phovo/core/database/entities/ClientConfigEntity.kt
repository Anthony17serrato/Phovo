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
    // TODO Server ID and server name must be non nullable, manual entries must be verified valid
    //  before they are stored in the client config as valid
    /**
     * Identity of the paired server, from its mDNS TXT record. Null when pairing from a manually
     * entered address, where nothing has told us who is on the other end yet.
     */
    @ColumnInfo(name = "server_id")
    val serverId: String? = null,
    /**
     * Last name the server reported for itself. A cache, not configuration: it starts as the mDNS
     * service name seen while pairing and is overwritten by every health probe, since the server is
     * the only authority on what it is called and the user can rename it at any time.
     */
    @ColumnInfo(name = "server_name")
    val serverName: String? = null
)
