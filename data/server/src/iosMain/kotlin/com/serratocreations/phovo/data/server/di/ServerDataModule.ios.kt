package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.core.common.di.MAIN_APPLICATION_SCOPE
import com.serratocreations.phovo.data.server.data.NoOpServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.data.server.IosServerDiscoveryManager
import com.serratocreations.phovo.data.server.ServerDiscoveryManager
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.arrayOf

internal actual fun getAndroidDesktopIosModules(): Module = module {
    single<ServerConfigManager> { NoOpServerConfigManager() }
    single<ServerConfigRepository> { IosAndroidServerConfigRepository(get()) } binds arrayOf(IosAndroidServerConfigRepository::class, ServerConfigRepository::class)
    single<ServerDiscoveryManager> {
        IosServerDiscoveryManager(
            get(),
            get(MAIN_APPLICATION_SCOPE),
            get()
        )
    }
}