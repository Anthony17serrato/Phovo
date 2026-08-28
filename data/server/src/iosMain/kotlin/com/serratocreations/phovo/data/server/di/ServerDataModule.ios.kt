package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.MAIN_APPLICATION_SCOPE
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.data.server.IosServerDiscoveryManager
import com.serratocreations.phovo.data.server.ServerAddressResolver
import com.serratocreations.phovo.data.server.ServerDiscoveryManager
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.arrayOf

internal actual fun getAndroidDesktopIosModules(): Module = module {
    single<ServerConfigRepository> { IosAndroidServerConfigRepository(get()) } binds arrayOf(IosAndroidServerConfigRepository::class, ServerConfigRepository::class)
    single<ServerDiscoveryManager> {
        IosServerDiscoveryManager(
            get(),
            get(MAIN_APPLICATION_SCOPE),
            get()
        )
    }

    single {
        ServerAddressResolver(
            serverConfigRepository = get(),
            remoteMediaRepository = get(),
            serverDiscoveryManager = get(),
            // Not the main scope: this reads and writes the database. Only NSNetService needs the
            // main run loop, and that stays inside the discovery manager it belongs to.
            applicationScope = get(APPLICATION_SCOPE),
            logger = get()
        )
    }
}