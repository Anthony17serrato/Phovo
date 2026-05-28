package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.model.MediaItemWithThumbnails
import kotlinx.coroutines.flow.Flow

interface GetPhotosFeedWithThumbnailsUseCase {

    operator fun invoke(): Flow<List<MediaItemWithThumbnails>>
}