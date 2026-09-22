package com.serratocreations.phovo.core.workmanager

import kotlinx.coroutines.flow.Flow

/**
 * Multiplatform background work scheduler, modelled on the androidx WorkManager API.
 *
 * Android delegates to androidx.work. iOS is backed by BGTaskScheduler over a persisted queue. On
 * both platforms enqueued work survives process death, which is why workers are named by a string
 * id resolved through [WorkerRegistry] rather than passed in as instances.
 *
 * All work is unique work. There is no anonymous enqueue, because anonymous work cannot be
 * reconciled after a restart.
 */
interface PhovoWorkManager {
    /**
     * Enqueues [request] under [uniqueWorkName], resolving a collision with [policy].
     */
    fun enqueueUniqueWork(
        uniqueWorkName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest
    )

    /**
     * Enqueues repeating [request] under [uniqueWorkName], resolving a collision with [policy].
     */
    fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        policy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest
    )

    /** Cancels work enqueued under [uniqueWorkName]. A run already in flight is cancelled. */
    fun cancelUniqueWork(uniqueWorkName: String)

    /** Cancels every piece of work carrying [tag]. */
    fun cancelAllWorkByTag(tag: String)

    /** Emits whenever the state of [uniqueWorkName] changes. Emits null when no such work exists. */
    fun getWorkInfoFlow(uniqueWorkName: String): Flow<WorkInfo?>

    /** One shot read of [uniqueWorkName], or null when no such work exists. */
    suspend fun getWorkInfo(uniqueWorkName: String): WorkInfo?
}
