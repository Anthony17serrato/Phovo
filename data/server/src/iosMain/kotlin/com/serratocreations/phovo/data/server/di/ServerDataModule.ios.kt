package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.core.common.di.MAIN_APPLICATION_SCOPE
import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import com.serratocreations.phovo.core.serverconfig.ServerEndpointResolver
import com.serratocreations.phovo.data.server.IosServerDiscoveryManager
import com.serratocreations.phovo.data.server.ServerDiscoveryManager
import com.serratocreations.phovo.data.server.ServerEndpointResolverImpl
import com.serratocreations.phovo.data.server.ServerHealthProbe
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
    single { ServerHealthProbe(createHealthProbeClient()) }
    single<ServerEndpointResolver> {
        ServerEndpointResolverImpl(
            serverConfigRepository = get(),
            serverDiscoveryManager = get(),
            healthProbe = get(),
            // Discovery is driven off the main run loop on iOS, so resolution shares that scope.
            applicationScope = get(MAIN_APPLICATION_SCOPE),
            logger = get()
        )
    }
}