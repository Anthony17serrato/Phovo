package com.serratocreations.phovo.core.workmanager

/** Conditions that must hold before a worker is allowed to run. */
data class Constraints(
    val requiredNetworkType: NetworkType = NetworkType.NONE,
    val requiresCharging: Boolean = false,
    val requiresBatteryNotLow: Boolean = false
) {
    companion object {
        val NONE = Constraints()
    }
}

enum class NetworkType {
    /** No network requirement. */
    NONE,

    /** Any network, metered or not. */
    CONNECTED,

    /** An unmetered network, e.g. wifi. */
    UNMETERED
}
