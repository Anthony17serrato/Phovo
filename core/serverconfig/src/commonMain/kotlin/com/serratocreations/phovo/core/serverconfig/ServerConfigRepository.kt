package com.serratocreations.phovo.core.serverconfig

import com.serratocreations.phovo.core.model.ServerConfig
import kotlinx.coroutines.flow.Flow

/**
 * What a client and a server both have: a config to observe. Pairing belongs to the client and
 * lives on [IosAndroidServerConfigRepository] — putting it here forced the desktop server to carry
 * empty overrides for calls it can never service.
 */
interface ServerConfigRepository {
    fun observeServerConfig(): Flow<ServerConfig?>
}
