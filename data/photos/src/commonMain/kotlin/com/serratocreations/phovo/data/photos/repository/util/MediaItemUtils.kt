package com.serratocreations.phovo.data.photos.repository.util

import com.serratocreations.phovo.data.photos.repository.model.MediaImageItem
import com.serratocreations.phovo.data.photos.repository.model.MediaItem
import com.serratocreations.phovo.data.photos.repository.model.MediaVideoItem

fun List<MediaItem>.segregate(): Pair<List<MediaVideoItem>, List<MediaImageItem>> {
    val processedImages: MutableList<MediaImageItem> = mutableListOf()
    val processedVideos: MutableList<MediaVideoItem> = mutableListOf()
    this.forEach { mediaItem ->
        when (mediaItem) {
            is MediaImageItem -> processedImages.add(mediaItem)
            is MediaVideoItem -> processedVideos.add(mediaItem)
        }
    }
    return processedVideos to processedImages
}