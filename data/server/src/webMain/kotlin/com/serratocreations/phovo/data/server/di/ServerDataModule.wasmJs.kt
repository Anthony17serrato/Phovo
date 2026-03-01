package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.data.server.data.NoOpServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.data.server.data.repository.IosAndroidWasmServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosWasmModules(): Module = module {
    single<ServerConfigManager> { NoOpServerConfigManager() }
    factory<ServerConfigRepository> { IosAndroidWasmServerConfigRepository() }
}