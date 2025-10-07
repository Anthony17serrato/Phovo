package com.serratocreations.phovo.core.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.serratocreations.phovo.core.model.MediaType
import kotlin.uuid.ExperimentalUuidApi

// TODO: Add md5 hash for modification check
@OptIn(ExperimentalUuidApi::class)
@Entity
data class MediaItemEntity(
    /** Insert methods treat 0 as not set for auto-generating keys */
    @PrimaryKey val localUuid: String,
    val remoteUuid: String?,
    // val md5Hash: String,
    /** Uri for accessing the asset locally on device */
    val remoteThumbnailUri: String?,
    val fileName: String,
    val timeStampUtcMs: Long,
    val timeOffsetMs: Long,
    val size: Int,
    // TODO: Consider separate entities for video and image items
    val mediaType: MediaType,
    val videoDurationMs: Long?,
)

@Entity
data class MediaItemUriEntity(
    @PrimaryKey
    val mediaUuid: String,
    val uri: String
)

data class MediaItemWithUriEntity(
    @Embedded val mediaItemEntity: MediaItemEntity,
    @Relation(
        parentColumn = "localUuid",
        entityColumn = "mediaUuid"
    )
    val mediaItemUri: MediaItemUriEntity
)