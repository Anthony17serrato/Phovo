package com.serratocreations.phovo.core.workmanager

/**
 * Turns a work request into long-running work with a user-visible progress surface.
 *
 * Both platforms hand out an extended background window only in exchange for showing the user what
 * the app is doing, so this carries the text they each display:
 *
 *  - **Android** promotes the worker to a foreground service (`setForeground`) with an ongoing
 *    notification. The ordinary ten minute JobScheduler deadline stops applying. Android 15 and
 *    above budget `dataSync` foreground services to roughly six hours per day.
 *  - **iOS 26 and above** submits a `BGContinuedProcessingTaskRequest`, which starts immediately
 *    and keeps running after the user leaves the app, with system progress UI. Below iOS 26 the
 *    request falls back to the ordinary `BGProcessingTask` queue.
 *
 * Report progress from the worker with [PhovoWorker.setProgress]. iOS requires it: the scheduler
 * may forcibly expire a continued processing task that looks stalled.
 */
data class LongRunningInfo(
    /** Short label for the notification title and the iOS task title, e.g. "Backing up photos". */
    val title: String,
    /** Secondary line, e.g. "12 of 340". Update it with [PhovoWorker.setProgress]. */
    val subtitle: String
) {
    init {
        require(title.isNotBlank()) { "Long-running work needs a title to show the user." }
    }
}
