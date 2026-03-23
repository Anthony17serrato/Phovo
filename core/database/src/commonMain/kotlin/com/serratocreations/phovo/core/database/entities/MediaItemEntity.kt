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
    /**
     * Uri where the asset can be access this may be a local or remote file depending on if the
     * client has the file stored locally
     */
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