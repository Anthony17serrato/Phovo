package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.core.database.di.getDatabaseModule
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManagerImpl
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.DesktopServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerEventsRepository
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosWasmModules(): org.koin.core.module.Module = module {
    includes(getDatabaseModule())

    factory {
        DesktopServerConfigRepository(get())
    } binds arrayOf(DesktopServerConfigRepository::class, ServerConfigRepository::class)

    single { ServerEventsRepository() }

    single<ServerConfigManager> {
        DesktopServerConfigManagerImpl(
            get(),
            get(),
            get(),
            get(),
            get(APPLICATION_SCOPE),
            get(IO_DISPATCHER)
        )
    }
}