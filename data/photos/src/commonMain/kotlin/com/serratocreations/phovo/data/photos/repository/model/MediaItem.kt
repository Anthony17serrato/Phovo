package com.serratocreations.phovo.data.photos.repository.model

import kotlinx.datetime.LocalDateTime

sealed interface MediaItem {
    val localUuid: String
    val remoteUuid: String?
    /** The location where the media can be accessed */
    val assetLocation: LocalOrRemoteAsset
    val fileName: String
    val dateInFeed: LocalDateTime
    // TODO Does size need to be long?
    val size: Int
}
