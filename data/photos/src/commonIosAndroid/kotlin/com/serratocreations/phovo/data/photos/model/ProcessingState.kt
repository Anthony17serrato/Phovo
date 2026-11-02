package com.serratocreations.phovo.data.photos.model

/**
 * How far a processing pass has got. [processedCount] counts every item the pass has handled,
 * whether it was processed, failed, skipped or had been processed before, so it reaches
 * [foundCount] when the pass is done.
 */
data class ProcessingProgress(
    val processedCount: Int = 0,
    val foundCount: Int = 0
)

sealed interface ProcessingState {
    val progress: ProcessingProgress

    data class Running(override val progress: ProcessingProgress = ProcessingProgress()) : ProcessingState

    data class Done(override val progress: ProcessingProgress) : ProcessingState
}
