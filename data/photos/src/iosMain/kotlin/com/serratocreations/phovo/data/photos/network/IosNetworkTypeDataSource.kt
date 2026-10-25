package com.serratocreations.phovo.data.photos.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue

/**
 * Network.framework path monitor. Classifies a path the way IosConstraintMonitor in
 * :core:workmanager does for its UNMETERED constraint, so the two agree.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosNetworkTypeDataSource : NetworkTypeDataSource {

    override fun observeConnectionType(): Flow<ConnectionType> = callbackFlow {
        // The monitor calls back once with the current path as soon as it starts.
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val type = when {
                path == null || nw_path_get_status(path) != nw_path_status_satisfied -> ConnectionType.NONE
                // Expensive means cellular or a personal hotspot; constrained means Low Data Mode.
                nw_path_is_expensive(path) || nw_path_is_constrained(path) -> ConnectionType.METERED
                else -> ConnectionType.UNMETERED
            }
            trySend(type)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0uL))
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }.conflate().distinctUntilChanged()
}
