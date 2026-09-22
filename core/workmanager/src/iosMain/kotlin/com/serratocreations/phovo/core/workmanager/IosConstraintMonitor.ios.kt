package com.serratocreations.phovo.core.workmanager

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue
import kotlin.concurrent.Volatile

/**
 * Answers whether a record's [Constraints] currently hold.
 *
 * Android gets this from the platform for free. On iOS the background task request carries its own
 * coarse constraints, but the foreground drain has to check for itself, and BGProcessingTask's
 * constraints are advisory enough that it is worth re checking before running anything.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosConstraintMonitor {

    @Volatile
    private var networkSatisfied: Boolean = false

    @Volatile
    private var networkUnmetered: Boolean = false

    private val monitor = nw_path_monitor_create()

    fun start() {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val satisfied = path != null && nw_path_get_status(path) == nw_path_status_satisfied
            networkSatisfied = satisfied
            // Expensive means cellular or personal hotspot; constrained means Low Data Mode. Either
            // one disqualifies a path from being treated as unmetered.
            networkUnmetered = satisfied &&
                !nw_path_is_expensive(path) &&
                !nw_path_is_constrained(path)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0uL))
        nw_path_monitor_start(monitor)

        // Required before batteryState and batteryLevel report anything other than unknown / -1.
        UIDevice.currentDevice.batteryMonitoringEnabled = true
    }

    fun isSatisfied(constraints: Constraints): Boolean {
        val networkOk = when (constraints.requiredNetworkType) {
            NetworkType.NONE -> true
            NetworkType.CONNECTED -> networkSatisfied
            NetworkType.UNMETERED -> networkUnmetered
        }
        if (!networkOk) return false

        val device = UIDevice.currentDevice
        if (constraints.requiresCharging) {
            val state = device.batteryState
            val charging = state == UIDeviceBatteryState.UIDeviceBatteryStateCharging ||
                state == UIDeviceBatteryState.UIDeviceBatteryStateFull
            if (!charging) return false
        }

        if (constraints.requiresBatteryNotLow) {
            val level = device.batteryLevel
            // batteryLevel is -1 when monitoring is unavailable, e.g. in the simulator. Treat an
            // unknown level as fine rather than blocking every constrained worker forever.
            if (level in 0f..LOW_BATTERY_THRESHOLD) return false
        }

        return true
    }

    private companion object {
        /** Mirrors the level at which Android reports battery low. */
        const val LOW_BATTERY_THRESHOLD = 0.15f
    }
}
