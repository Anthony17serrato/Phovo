package com.serratocreations.phovo.data.photos.repository.model

sealed interface SyncQueueable {
    val uuid: String
}

data class SyncVideo(
    override val uuid: String
): SyncQueueable

data class SyncImage(
    override val uuid: String
): SyncQueueable