package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.data.server.data.NoOpServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.data.server.data.repository.IosAndroidWasmServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.arrayOf

internal actual fun getAndroidDesktopIosWasmModules(): Module = module {
    single<ServerConfigManager> { NoOpServerConfigManager() }
    single<ServerConfigRepository> { IosAndroidWasmServerConfigRepository() } binds arrayOf(IosAndroidWasmServerConfigRepository::class, ServerConfigRepository::class)
}