package com.serratocreations.phovo.core.common.performance

/**
 * How much of the machine background media processing(metadata extraction, hashing and thumbnail
 * generation) is allowed to consume.
 *
 * Background processing competes with the UI for CPU. On desktop this is especially true of
 * thumbnail generation, which runs FFmpeg as an OS subprocess whose threads are scheduled outside
 * of the JVM's control and therefore compete directly with the AWT event thread and the Skiko
 * render thread. Every mode leaves headroom for the UI, higher modes simply leave less.
 *
 * TODO Surface this as a user facing setting and persist it with the rest of the client config.
 *  Until then DI provides [DEFAULT]. Because [ProcessingCpuBudget] is resolved once at startup,
 *  making this configurable will also require the consumers(worker pool size and the thumbnail
 *  process semaphore) to be re-sized when the mode changes.
 */
enum class ProcessingPerformanceMode {
    /** Roughly a quarter of the machine. For working while a large import runs. */
    Low,

    /** Roughly half of the machine. */
    Medium,

    /** As much of the machine as can be used while keeping the UI responsive. */
    High;

    companion object {
        val DEFAULT: ProcessingPerformanceMode = High
    }
}

/**
 * Concrete concurrency limits derived from a [ProcessingPerformanceMode] and the size of the
 * machine. Resolve once via [forMode] and inject, rather than reading
 * `availableProcessors` at each call site.
 */
data class ProcessingCpuBudget(
    /** Total cores background processing may keep busy. */
    val cpuBudget: Int,
    /** Number of media processing workers to run in parallel. */
    val workerCount: Int,
    /** Ceiling on simultaneously running thumbnail(FFmpeg) subprocesses. */
    val maxConcurrentThumbnailProcesses: Int,
    /** Threads a single thumbnail(FFmpeg) subprocess may use. */
    val threadsPerThumbnailProcess: Int
) {
    init {
        require(threadsPerThumbnailProcess * maxConcurrentThumbnailProcesses <= cpuBudget) {
            "Thumbnail generation may not be allowed to exceed the CPU budget of $cpuBudget"
        }
    }

    companion object {
        /**
         * Thumbnail work parallelises far better across files than within a single file(scaling one
         * image barely benefits from a second thread), so the budget is spent on running many
         * single threaded subprocesses. Kept as a named knob because video thumbnails, which decode
         * many frames to pick one, would benefit from raising it.
         */
        private const val THREADS_PER_THUMBNAIL_PROCESS = 1

        /**
         * Cores [ProcessingPerformanceMode.High] holds back for the UI. Proportional so that a
         * fixed reserve is not a rounding error on a workstation, but clamped at both ends: below
         * [MIN_UI_RESERVED_CORES] the render and event threads have nowhere to run, and above
         * [MAX_UI_RESERVED_CORES] we would be idling cores the UI has no use for.
         */
        private const val MIN_UI_RESERVED_CORES = 2
        private const val MAX_UI_RESERVED_CORES = 4

        fun forMode(
            mode: ProcessingPerformanceMode,
            availableProcessors: Int
        ): ProcessingCpuBudget {
            val cores = maxOf(1, availableProcessors)
            val uiReservedCores = (cores / 8).coerceIn(MIN_UI_RESERVED_CORES, MAX_UI_RESERVED_CORES)
            val cpuBudget = when (mode) {
                ProcessingPerformanceMode.Low -> cores / 4
                ProcessingPerformanceMode.Medium -> cores / 2
                ProcessingPerformanceMode.High -> cores - uiReservedCores
            }
                // Below four cores every mode collapses to a single worker, which still leaves a
                // core for the UI on a dual core machine. On a single core machine there is no
                // split to make, so favour making progress.
                .coerceAtLeast(1)

            val threadsPerProcess = THREADS_PER_THUMBNAIL_PROCESS.coerceAtMost(cpuBudget)
            return ProcessingCpuBudget(
                cpuBudget = cpuBudget,
                // A worker spends most of its life waiting on IO or on its FFmpeg child, so
                // matching the budget keeps the pipeline fed without oversubscribing it.
                workerCount = cpuBudget,
                maxConcurrentThumbnailProcesses = maxOf(1, cpuBudget / threadsPerProcess),
                threadsPerThumbnailProcess = threadsPerProcess
            )
        }
    }
}
