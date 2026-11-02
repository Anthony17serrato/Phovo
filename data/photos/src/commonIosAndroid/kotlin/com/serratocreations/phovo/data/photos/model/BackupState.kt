package com.serratocreations.phovo.data.photos.model

import com.serratocreations.phovo.data.photos.repository.model.SyncByteProgress
import com.serratocreations.phovo.data.photos.repository.model.SyncState

sealed interface BackupState {
    /** Nothing is being processed or uploaded */
    val isIdle: Boolean

    /**
     * Phovo can't read the gallery, so nothing is processed or backed up until the user grants
     * access. Idle, so anything waiting for a backup to finish stops waiting.
     */
    data object MediaAccessRequired : BackupState {
        override val isIdle: Boolean = true
    }

    data class Active(
        val processing: ProcessingState,
        val syncState: SyncState
    ) : BackupState {
        val isProcessing: Boolean get() = processing is ProcessingState.Running

        override val isIdle: Boolean get() = !isProcessing && syncState !is SyncState.Syncing

        /**
         * How far the whole pass has got, for reporting to the OS.
         *
         * While there are bytes to upload, processing and sync each make up half, measured in the
         * sync's total bytes ([SyncByteProgress]), so both halves move as that total grows. With
         * nothing to upload, processing is all of it, measured in items. The units switch from
         * items to bytes once there is something to upload, which reads as a jump and still counts
         * as movement.
         */
        fun progress(): BackupProgress = when (syncState) {
            // Nothing is uploading, so processing is all of the progress.
            is SyncState.Waiting, is SyncState.SyncComplete -> processingProgress(total = foundCount)
            is SyncState.Syncing -> {
                val syncBytes = syncState.syncByteProgress()
                if (syncBytes.totalBytes == 0L) {
                    // Syncing, but nothing to upload yet.
                    processingProgress(total = foundCount)
                } else {
                    processingProgress(total = syncBytes.totalBytes) + syncProgress(syncBytes)
                }
            }
        }

        private val foundCount: Long get() = processing.progress.foundCount.toLong()

        /** Processing's share of the progress, scaled to [total]. */
        private fun processingProgress(total: Long): BackupProgress = when (processing) {
            is ProcessingState.Done -> BackupProgress.finished(total)
            is ProcessingState.Running -> {
                val processed = processing.progress.processedCount.toLong()
                // The same fraction as processed / found, in [total]'s units.
                val completed = if (foundCount == 0L) 0L else (processed.toDouble() / foundCount * total).toLong()
                BackupProgress.running(completed = completed, total = total)
            }
        }

        /** Sync's share of the progress, in bytes. Only [SyncState.SyncComplete] finishes sync. */
        private fun syncProgress(syncBytes: SyncByteProgress): BackupProgress =
            BackupProgress.running(completed = syncBytes.completedBytes, total = syncBytes.totalBytes)
    }
}

/** [completed] out of [total]. Reaching [total] means done: the iOS progress bar shows it as full. */
data class BackupProgress(val completed: Long, val total: Long) {
    /** Two shares of the progress side by side. */
    operator fun plus(other: BackupProgress) =
        BackupProgress(completed = completed + other.completed, total = total + other.total)

    companion object {
        /** A phase that has finished. */
        fun finished(total: Long) = BackupProgress(completed = total, total = total)

        /**
         * A phase that is still running. Even when its work has caught up with [total] (every
         * known item handled, every known byte sent), more can still arrive, so it stays one
         * short of [total] and doesn't show as done.
         */
        fun running(completed: Long, total: Long): BackupProgress {
            val shownCompleted = when {
                total == 0L -> 0L
                completed < total -> completed
                else -> total - 1
            }
            return BackupProgress(completed = shownCompleted, total = total)
        }
    }
}
