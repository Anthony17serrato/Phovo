package com.serratocreations.phovo.data.photos.local.model

import io.github.vinceglb.filekit.PlatformFile

/** What a scan of the local library reports, in order. */
sealed interface ScanEvent {
    /**
     * A query matched [count] assets. Emitted before that query's [Item]s, and once per query, so
     * counts add up across queries.
     */
    data class Found(val count: Int) : ScanEvent

    /** An asset to process. */
    data class Item(val file: PlatformFile) : ScanEvent

    /** An asset counted by [Found] that won't be sent for processing, such as an iCloud-only one. */
    data object Skipped : ScanEvent
}
