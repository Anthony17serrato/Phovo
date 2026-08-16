package com.serratocreations.phovo.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ClientConfigEntity(
    // Since only one server URL is active at a time, we use a single row
    // with a fixed primary key of 1 and an onConflict REPLACE strategy.
    @PrimaryKey val id: Long = 1,
    /**
     * The last address the server was reachable at. This is a cache, not the identity of the
     * connection — an address is a DHCP lease and is expected to change. [serverId] is what
     * actually identifies the server, and the resolver rewrites this column whenever it finds the
     * server somewhere new.
     */
    val serverUrl: String,
    /**
     * Identity of the paired server, as reported by its health endpoint and mDNS TXT record.
     * Null only between a manual pairing being saved and its first successful health probe, after
     * which the client adopts the identity the server reports.
     */
    @ColumnInfo(name = "server_id")
    val serverId: String? = null,
    /** mDNS service instance name, used as a hint when re-browsing for the server. */
    @ColumnInfo(name = "service_name")
    val serviceName: String? = null
)
