package com.serratocreations.phovo.data.photos.local.model

import io.github.vinceglb.filekit.PlatformFile

/** What a scan of the local library reports, in order. */
sealed interface ScanEvent {
    /**
     * The scan matched [count] assets in total, across every query it runs. Emitted once, before
     * any [Item] or [Skipped].
     */
    data class Found(val count: Int) : ScanEvent

    /** An asset to process. */
    data class Item(val file: PlatformFile) : ScanEvent

    /** An asset counted by [Found] that won't be sent for processing, such as an iCloud-only one. */
    data object Skipped : ScanEvent
}
