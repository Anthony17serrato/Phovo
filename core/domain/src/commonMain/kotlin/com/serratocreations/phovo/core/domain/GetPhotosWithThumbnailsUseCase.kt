package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.milliseconds

/**
 * While media is being processed the media table is invalidated on every processed item, which
 * makes the underlying media flow emit many times per second. Implementations sample the media
 * flow with this period *before* mapping so the per-item thumbnail resolution(which touches the
 * file system for every item in the library) runs at most twice a second instead of once per
 * processed item.
 */
internal val PHOTOS_FEED_SAMPLE_PERIOD = 500.milliseconds

interface GetPhotosFeedWithThumbnailsUseCase {

    operator fun invoke(): Flow<List<MediaItemWithThumbnails>>
}