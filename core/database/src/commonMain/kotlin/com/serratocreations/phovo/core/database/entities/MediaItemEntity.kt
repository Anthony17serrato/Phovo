package com.serratocreations.phovo.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.serratocreations.phovo.core.model.MediaType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// TODO: Add md5 hash for uniqueness check
@OptIn(ExperimentalUuidApi::class)
@Entity
data class MediaItemEntity(
    /** Insert methods treat 0 as not set for auto-generating keys */
    @PrimaryKey val localUuid: String = Uuid.random().toString(),
    val remoteUuid: String?,
    // val md5Hash: String,
    /** Uri for accessing the asset locally on device */
    // TODO: Uri needs to be moved to a separate table, client/server should not share this
    val localUri: String,
    val remoteUri: String?,
    val remoteThumbnailUri: String?,
    val fileName: String,
    val timeStampUtcMs: Long,
    val timeOffsetMs: Long,
    val size: Int,
    // TODO: Consider separate entities for video and image items
    val mediaType: MediaType,
    val videoDurationMs: Long?,
)