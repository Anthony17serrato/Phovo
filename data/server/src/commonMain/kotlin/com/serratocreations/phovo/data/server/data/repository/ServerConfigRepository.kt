package com.serratocreations.phovo.data.server.data.repository

import com.serratocreations.phovo.data.server.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IosAndroidWasmServerConfigRepository: ServerConfigRepository()

abstract class ServerConfigRepository {
    open fun observeServerConfig(): Flow<ServerConfig?> {
        // TODO("Implement networkDataSource for IOS/Android/Wasm")
        return flowOf(null)
    }
}