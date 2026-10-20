package com.serratocreations.phovo.data.photos.local.model

import com.serratocreations.phovo.data.photos.repository.model.MediaItem

sealed interface MediaProcessingResult {
    data class Success(val mediaItem: MediaItem) : MediaProcessingResult

    data class Failure(val reason: MediaProcessingFailure) : MediaProcessingResult
}

/** Why a local file could not be turned into a [MediaItem]. */
enum class MediaProcessingFailure {
    /** The file's MIME type is missing, or it is neither an image nor a video. */
    UnsupportedType,

    /** The width, height or orientation could not be read from the file. */
    UnreadableDimensions,

    /** The file's MediaStore row is gone, usually because it was deleted after the scan. */
    MissingFromLibrary,
    MissingCreationDate
}