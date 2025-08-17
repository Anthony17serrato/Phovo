package com.serratocreations.phovo.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// TODO: Add md5 hash for uniqueness check
@Entity
data class PhovoItemEntity(
    /** Insert methods treat 0 as not set for auto-generating keys */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // val md5Hash: String,
    /** Uri for accessing the asset locally on device */
    val localUri: String,
    val remoteUri: String?,
    val remoteThumbnailUri: String?,
    val name: String,
    val timeStampUtcMs: Long,
    val timeOffsetMs: Long,
    val size: Int,
    // TODO: Consider separate entities for video and image items
    val mediaType: MediaType,
    val videoDurationMs: Long?,
)

enum class MediaType { Image, Video }