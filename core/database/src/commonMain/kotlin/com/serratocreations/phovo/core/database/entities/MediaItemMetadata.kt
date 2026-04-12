package com.serratocreations.phovo.core.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.serratocreations.phovo.core.model.MediaType
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Entity
data class MediaItemMetadata(
    /** Sha-256 Hash **/
    @PrimaryKey val metadataAssetHash: String,
    val fileName: String,
    val timeStampUtcMs: Long,
    val timeOffsetMs: Long,
    val size: Long,
    // TODO: Consider separate entities for video and image items
    val mediaType: MediaType,
    val videoDurationMs: Long?,
)

@Entity
data class MediaItemLocationEntity(
    /** Sha-256 hash **/
    @PrimaryKey
    val assetHash: String,
    /**
     * Indicates the location where the asset is stored, In the event the asset location is
     * [AssetLocation.LocalAndRemote] then [uri] will always represent the local asset.
     */
    val assetLocation: AssetLocation,
    /**
     * Uri where the asset can be access this may be a local or remote file depending
     * on the value of [assetLocation].
     */
    val uri: String
)

/**
 * This includes both processed and unprocessed media items.
 * [mediaItemMetadata] is nullable in the case where [mediaItemLocation] has not been processed
 */
data class MediaItemWithMetadataIfExists(
    @Embedded val mediaItemLocation: MediaItemLocationEntity,
    @Relation(
        parentColumn = "assetHash",
        entityColumn = "metadataAssetHash"
    )
    val mediaItemMetadata: MediaItemMetadata?,
)

data class MediaItemWithMetadata(
    @Embedded val mediaItemMetadata: MediaItemMetadata,
    @Embedded(prefix = "uri_") val mediaItemLocation: MediaItemLocationEntity
)


internal const val LOCAL_SERIAL_ID = 0
/**
 * Because this enum is serialized into the database careful consideration is necessary
 * before making modifications to this class. Changes may require a database migration
 */
enum class AssetLocation(
    /** ID which is serialized into DB */
    val serialId: Int
) {
    Local(serialId = LOCAL_SERIAL_ID),
    Remote(serialId = 1),
    /**
     * Indicates the asset has been synchronized with the remote server
     */
    LocalAndRemote(serialId = 2);

    companion object {
        fun getFromSerialId(serialId: Int): AssetLocation {
            return entries.first { it.serialId == serialId }
        }
    }
}