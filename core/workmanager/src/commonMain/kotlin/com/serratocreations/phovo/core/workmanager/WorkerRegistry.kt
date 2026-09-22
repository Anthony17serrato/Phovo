package com.serratocreations.phovo.core.workmanager

/**
 * Binds a stable [workerId] to a way of creating the worker.
 *
 * Work outlives the process that enqueued it, so a persisted work record can only name its worker
 * by id. Consumers contribute one registration per worker to DI:
 *
 * ```
 * single { WorkerRegistration("media-sync") { MediaSyncWorker(get()) } }
 * ```
 *
 * Ids are part of the persisted format. Renaming one orphans any work already on disk.
 */
class WorkerRegistration(
    val workerId: String,
    val factory: () -> PhovoWorker
)

/** Resolves a [workerId] read back from persisted work into a runnable [PhovoWorker]. */
class WorkerRegistry(registrations: List<WorkerRegistration>) {
    private val factories: Map<String, () -> PhovoWorker> =
        registrations.associate { it.workerId to it.factory }

    /** Returns null when no worker is registered for [workerId], e.g. after a worker was removed. */
    fun create(workerId: String): PhovoWorker? = factories[workerId]?.invoke()
}
