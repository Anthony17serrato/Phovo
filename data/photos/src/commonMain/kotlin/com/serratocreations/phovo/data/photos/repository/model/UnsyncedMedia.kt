package com.serratocreations.phovo.data.photos.repository.model

/**
 * Snapshot of everything not yet synced. Both values grow as a scan finds new media and shrink as
 * items sync.
 */
data class UnsyncedMedia(
    val count: Int,
    /** Total size in bytes of the unsynced items */
    val bytes: Long
)
