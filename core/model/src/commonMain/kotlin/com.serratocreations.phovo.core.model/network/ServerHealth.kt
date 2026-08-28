package com.serratocreations.phovo.core.model.network

import kotlinx.serialization.Serializable

/**
 * A server's response to a health probe.
 *
 * [serverId] is what makes a client's stored connection durable. It identifies the server
 * independently of whatever address it currently holds, so a client can tell "my server moved" from
 * "some other machine now answers on that address".
 */
@Serializable
data class ServerHealth(
    val serverId: String,
    val serverName: String,
    val protocolVersion: Int = PhovoProtocol.VERSION
)

object PhovoProtocol {
    /** Bumped when the client/server contract changes in a way older peers cannot handle. */
    const val VERSION = 1
}
