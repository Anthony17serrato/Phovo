package com.serratocreations.phovo.core.workmanager

/**
 * Binds a stable [workerId] to a way of creating the worker.
 *
 * Work outlives the process that enqueued it, so a persisted work record can only name its worker
 * by id. Consumers contribute one registration per worker to DI, qualified by the worker id:
 *
 * ```
 * single(named("media-sync")) { WorkerRegistration("media-sync") { MediaSyncWorker(get()) } }
 * ```
 *
 * The qualifier is required. Unqualified singles of one type override each other in Koin, so
 * [WorkerRegistry] would only ever see the last registration.
 *
 * Ids are part of the persisted format. Renaming one orphans any work already on disk.
 */
class WorkerRegistration(
    val workerId: String,
    val factory: () -> PhovoWorker
)

/** Resolves a [workerId] read back from persisted work into a runnable [PhovoWorker]. */
class WorkerRegistry(registrations: List<WorkerRegistration>) {

    private val factories: Map<String, () -> PhovoWorker>

    init {
        // Ids are strings, so two modules can reach for the same one without the compiler
        // objecting. Left alone they would silently share queue entries, with the winner decided by
        // whatever order Koin happened to collect the registrations in. Fail at startup instead.
        val duplicates = registrations.groupBy { it.workerId }.filter { it.value.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "More than one WorkerRegistration for ${duplicates.joinToString { "'$it'" }}. " +
                "Each worker id may be registered once."
        }
        factories = registrations.associate { it.workerId to it.factory }
    }

    /** Returns null when no worker is registered for [workerId], e.g. after a worker was removed. */
    fun create(workerId: String): PhovoWorker? = factories[workerId]?.invoke()
}
