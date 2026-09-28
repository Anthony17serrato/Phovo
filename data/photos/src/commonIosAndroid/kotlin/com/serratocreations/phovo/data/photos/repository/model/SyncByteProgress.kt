package com.serratocreations.phovo.data.photos.repository.model

/**
 * How far the current backup has got, in bytes.
 *
 * Bytes rather than items because a background task has to keep reporting movement to stay alive,
 * and item counts only move when a whole file finishes: one large video can be many minutes with
 * no change. Bytes move as an upload is written.
 *
 * [totalBytes] grows while a scan is still finding media, so [completedBytes] / [totalBytes] can
 * fall back as well as rise. That is expected, and still counts as movement.
 */
data class SyncByteProgress(
    /** Bytes synced this session, plus bytes written so far for uploads still in flight. */
    val completedBytes: Long,
    /** Bytes synced this session, plus the size of everything not yet synced. */
    val totalBytes: Long
)
