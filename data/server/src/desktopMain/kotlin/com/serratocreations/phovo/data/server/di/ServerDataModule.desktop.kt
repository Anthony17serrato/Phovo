package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.core.common.di.ApplicationScope
import com.serratocreations.phovo.core.common.di.IoDispatcher
import com.serratocreations.phovo.core.database.dao.ServerConfigDao
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManagerImpl
import com.serratocreations.phovo.data.server.data.ServerConfigManager
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.DesktopServerConfigRepository
import com.serratocreations.phovo.data.server.data.repository.ServerEventsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

// TODO add database module after migration to remove annotations
@Module
internal actual class ServerDataPlatformModule {
    @Factory(binds = [DesktopServerConfigRepository::class, ServerConfigRepository::class])
    fun desktopServerConfigRepository(
        localDataSource: ServerConfigDao
    ) = DesktopServerConfigRepository(localDataSource)

    @Singleton(binds = [ServerConfigManager::class])
    fun desktopServerConfigManagerImpl(
        logger: PhovoLogger,
        serverConfigRepository: DesktopServerConfigRepository,
        serverEventsRepository: ServerEventsRepository,
        @ApplicationScope appScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) = DesktopServerConfigManagerImpl(
        logger,
        serverConfigRepository,
        serverEventsRepository,
        appScope,
        ioDispatcher
    )
}