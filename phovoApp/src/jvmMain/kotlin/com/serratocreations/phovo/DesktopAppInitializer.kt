package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManager
import com.serratocreations.phovo.data.server.data.repository.DesktopServerConfigRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class DesktopAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverConfigRepository: DesktopServerConfigRepository,
    private val desktopServerConfigManager: DesktopServerConfigManager,
    private val localMediaManager: LocalMediaManager
): AndroidDesktopIosAppInitializer(
    applicationScope,
    serverConfigRepository,
    localMediaManager
) {
    override fun initialize() {
        super.initialize()
        // Initialize FileKit
        FileKit.init(appId = "com.serratocreations.phovo")
        applicationScope.launch {
            val serverConfig = serverConfigRepository.observeServerConfig().first()
            // If this server has been configured restart it
            serverConfig?.let { availableServerConfig ->
                desktopServerConfigManager.configureDeviceAsServer(availableServerConfig)
            }
        }
        applicationScope.launch {
            // Suspends until a config is available(consider withTimeout)
            val backupDirectory = serverConfigRepository.observeServerConfig()
                .mapNotNull { it?.backupDirectory }
                .distinctUntilChanged()
                .first()
            localMediaManager.initMediaProcessing(backupDirectory.absolutePath())
        }
    }
}