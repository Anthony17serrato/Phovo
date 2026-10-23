package com.serratocreations.phovo.data.photos.network

import kotlinx.coroutines.flow.Flow

/**
 * What the device's current default network is good for.
 *
 * Mirrors what androidx.work checks for its UNMETERED constraint, so in-app sync and background
 * work agree on when uploading is fine: a network only counts once it is validated, i.e. actually
 * reaches the internet.
 */
enum class ConnectionType {
    /** No validated network. */
    NONE,

    /** Validated, but costs the user: cellular, a metered hotspot, or iOS Low Data Mode. */
    METERED,

    /** Validated and free to use, e.g. normal Wi-Fi. */
    UNMETERED
}

/** Observes the device's default network. */
interface NetworkTypeDataSource {
    /**
     * Emits the current [ConnectionType] on collection, then again whenever it changes. Each
     * collector registers its own platform callback, released when collection stops.
     */
    fun observeConnectionType(): Flow<ConnectionType>
}
