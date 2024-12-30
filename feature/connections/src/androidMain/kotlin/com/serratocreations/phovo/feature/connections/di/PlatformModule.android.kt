package com.serratocreations.phovo.feature.connections.di

import com.serratocreations.phovo.feature.connections.data.NoOpServerConfigManager
import com.serratocreations.phovo.feature.connections.data.ServerConfigManager
import org.koin.dsl.module

internal actual fun platformModule() = module {
    single<ServerConfigManager> { NoOpServerConfigManager() }
}