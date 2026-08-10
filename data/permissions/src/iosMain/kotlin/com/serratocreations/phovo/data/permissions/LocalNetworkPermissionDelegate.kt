package com.serratocreations.phovo.data.permissions

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * A single, terminal outcome reported by the platform local-network probe.
 *
 * The Swift implementation reports only what the system told it. All interpretation — timeouts,
 * cancellation and serialising concurrent probes — is handled in [IosPermissionRepository] so the
 * asynchronous logic stays in coroutines rather than in Swift.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("LocalNetworkProbeEvent", exact = true)
enum class LocalNetworkProbeEvent {
    /** The probe service published, which is only possible once access is authorized. */
    Published,

    /** The system explicitly denied local network access. */
    Denied,

    /**
     * The probe could not determine an answer — publishing or browsing failed for a reason other
     * than an explicit policy denial, e.g. no usable network interface. Inconclusive, not a denial.
     *
     * Entry names are deliberately single words: the ObjC exporter lowercases them wholesale, so
     * `PublishFailed` would reach Swift as `publishfailed`.
     */
    Failed
}

@OptIn(ExperimentalObjCName::class)
@ObjCName("LocalNetworkPermissionDelegate", exact = true)
interface LocalNetworkPermissionDelegate {
    /**
     * Starts a probe. [onEvent] is invoked at most once, on the main thread. If no outcome is ever
     * determined it is never invoked at all — callers are responsible for applying a timeout and
     * calling [cancelProbe].
     */
    fun startProbe(onEvent: (LocalNetworkProbeEvent) -> Unit)

    /** Tears down any in-flight probe. Safe to call when none is running. */
    fun cancelProbe()
}
