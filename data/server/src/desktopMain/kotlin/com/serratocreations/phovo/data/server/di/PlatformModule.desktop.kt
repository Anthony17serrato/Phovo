package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManagerImpl
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual fun platformModule() = module {
    single<ServerConfigManager> {
        DesktopServerConfigManagerImpl(
            serverConfigRepository = get(),
            serverEventsRepository = get(),
            appScope = get(named(APPLICATION_SCOPE)),
            ioDispatcher = get(named(IO_DISPATCHER))
        )
    }
}