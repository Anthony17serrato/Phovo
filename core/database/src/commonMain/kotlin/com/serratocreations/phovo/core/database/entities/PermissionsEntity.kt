package com.serratocreations.phovo.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PermissionsEntity(
    @PrimaryKey val permissionId: String,
    val isPermanentlyDenied: Boolean
)