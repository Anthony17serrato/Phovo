package com.serratocreations.phovo.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PermissionStateEntity {
    Allowed,
    Denied,
    PermanentlyDenied
}

@Entity
data class PermissionsEntity(
    @PrimaryKey val permissionId: String,
    val state: PermissionStateEntity
)