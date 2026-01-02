package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManager
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DesktopAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverConfigRepository: ServerConfigRepository,
    private val desktopServerConfigManager: DesktopServerConfigManager,
    localMediaManager: LocalMediaManager
): AndroidDesktopIosAppInitializer(
    applicationScope,
    serverConfigRepository,
    localMediaManager
) {
    override fun initialize() {
        super.initialize()
        applicationScope.launch {
            val serverConfig = serverConfigRepository.observeServerConfig().first()
            // If this server has been configured restart it
            serverConfig?.let { availableServerConfig ->
                desktopServerConfigManager.configureDeviceAsServer(availableServerConfig)
            }
        }
    }
}