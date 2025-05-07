package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.data.server.data.NoOpServerConfigManager
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.data.server.data.repository.IosAndroidWasmServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.serratocreations.phovo.data.server")
internal actual class ServerDataPlatformModule {
    @Singleton(binds = [ServerConfigManager::class])
    fun serverConfigManager() = NoOpServerConfigManager()

    @Factory(binds = [ServerConfigRepository::class])
    fun serverConfigRepository() = IosAndroidWasmServerConfigRepository()
}