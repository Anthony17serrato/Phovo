package com.serratocreations.phovo.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ClientConfigEntity(
    // Since only one server URL is active at a time, we use a single row
    // with a fixed primary key of 1 and an onConflict REPLACE strategy.
    @PrimaryKey val id: Long = 1,
    val serverUrl: String
)
