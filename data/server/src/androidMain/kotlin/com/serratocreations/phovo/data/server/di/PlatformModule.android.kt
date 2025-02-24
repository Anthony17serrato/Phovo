package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.data.server.data.NoOpServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import org.koin.dsl.module

internal actual fun platformModule() = module {
    single<ServerConfigManager> { NoOpServerConfigManager() }
}