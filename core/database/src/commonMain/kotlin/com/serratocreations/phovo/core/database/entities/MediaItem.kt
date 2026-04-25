package com.serratocreations.phovo.core.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.serratocreations.phovo.core.model.MediaType
import kotlin.uuid.ExperimentalUuidApi

/**
 * Represents a media item which has been processed for display on the feed,
 * This means a metadata worker has done the extraction of metadata.
 */
@OptIn(ExperimentalUuidApi::class)
@Entity
data class MediaItemMetadataEntity(
    /** Sha-256 Hash **/
    @PrimaryKey val assetHash: String,
    val isSynced: Boolean,
    val fileName: String,
    val timeStampUtcMs: Long,
    val timeOffsetMs: Long,
    val size: Long,
    // TODO: Consider separate entities for video and image items
    val mediaType: MediaType,
    val videoDurationMs: Long?,
)

@Entity
data class LocalMediaEntity(
    /** Sha-256 hash **/
    @PrimaryKey
    val assetHash: String,
    /**
     * Uri where the asset can be accessed locally.
     */
    val localUri: String
)

/**
 * Model for media items which have been processed
 */
data class MediaItemWithMetadata(
    @Embedded val mediaItemMetadataEntity: MediaItemMetadataEntity,
    @Relation(
        parentColumn = "assetHash",
        entityColumn = "assetHash"
    )
    /** If the asset is available locally it will be non-null */
    val localLocation: LocalMediaEntity?
)

/**
 * Model for media items which have been processed and exist locally
 */
data class LocalMediaItemWithMetadata(
    @Embedded val mediaItemMetadataEntity: MediaItemMetadataEntity,
    @Embedded(prefix = "local_") val localLocation: LocalMediaEntity
)