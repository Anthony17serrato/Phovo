package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.data.server.data.NoOpServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.core.serverconfig.WasmServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.discovery.ServerDiscoveryManager
import com.serratocreations.phovo.core.serverconfig.discovery.WebServerDiscoveryManager
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosWasmModules(): Module = module {
    single<ServerConfigManager> { NoOpServerConfigManager() }
    single<ServerConfigRepository> { WasmServerConfigRepository() } binds arrayOf(WasmServerConfigRepository::class, ServerConfigRepository::class)
    single<ServerDiscoveryManager> { WebServerDiscoveryManager() }
}