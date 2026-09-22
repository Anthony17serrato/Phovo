package com.serratocreations.phovo.core.workmanager

import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * Durable store for [PersistedWorkRecord]s, kept in NSUserDefaults as a single JSON array.
 *
 * The whole queue is expected to hold a handful of entries (it is unique work only, one record per
 * name), so rewriting the array on every change is cheaper than the bookkeeping a real database
 * would cost, and it keeps this module free of Room and KSP.
 */
internal class WorkRecordStore(
    private val defaults: NSUserDefaults,
    private val logger: PhovoLogger
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _records = MutableStateFlow(load())
    val records: StateFlow<List<PersistedWorkRecord>> = _records.asStateFlow()

    /** Applies [transform] to the queue and writes the result back to disk. */
    fun update(transform: (List<PersistedWorkRecord>) -> List<PersistedWorkRecord>) {
        _records.update(transform)
        persist(_records.value)
    }

    fun find(uniqueWorkName: String): PersistedWorkRecord? =
        _records.value.firstOrNull { it.uniqueWorkName == uniqueWorkName }

    private fun load(): List<PersistedWorkRecord> {
        val raw = defaults.stringForKey(KEY) ?: return emptyList()
        return try {
            json.decodeFromString<List<PersistedWorkRecord>>(raw).map { record ->
                // A record left RUNNING means the process died mid run. Put it back in the queue
                // rather than stranding it in a state nothing will ever move it out of.
                if (record.state == WorkState.RUNNING) record.copy(state = WorkState.ENQUEUED) else record
            }
        } catch (e: Exception) {
            // A queue we cannot read is worse than an empty one; dropping it loses pending work but
            // keeps the app usable, and consumers re enqueue their periodic work on every launch.
            logger.e(e) { "Could not read the persisted work queue, starting empty." }
            emptyList()
        }
    }

    private fun persist(records: List<PersistedWorkRecord>) {
        try {
            defaults.setObject(json.encodeToString(records), KEY)
        } catch (e: Exception) {
            logger.e(e) { "Could not persist the work queue." }
        }
    }

    private companion object {
        const val KEY = "com.serratocreations.phovo.core.workmanager.records"
    }
}
