package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import kotlinx.coroutines.flow.Flow

interface GetPhotosFeedWithThumbnailsUseCase {
    companion object {
        protected const val LOW_RES_THUMBNAIL_DIR = "low_res_thumbnails"
        protected const val LOW_RES_THUMBNAIL_API = "low_res_thumbnails/"
        protected const val HIGH_RES_THUMBNAIL_API = "high_res_thumbnails/"
        protected const val HIGH_RES_THUMBNAIL_DIR = "high_res_thumbnails"
    }

    operator fun invoke(): Flow<List<MediaItemWithThumbnails>>
}