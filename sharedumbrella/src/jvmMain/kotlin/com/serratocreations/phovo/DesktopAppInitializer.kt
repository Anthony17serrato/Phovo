package com.serratocreations.phovo

import com.serratocreations.phovo.core.serverconfig.DesktopServerConfigRepository
import com.serratocreations.phovo.data.photos.DesktopLocalMediaManager
import com.serratocreations.phovo.data.server.data.DesktopServerConfigManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class DesktopAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverConfigRepository: Lazy<DesktopServerConfigRepository>,
    private val desktopServerConfigManager: Lazy<DesktopServerConfigManager>,
    private val localMediaManager: Lazy<DesktopLocalMediaManager>
): AndroidDesktopIosAppInitializer() {
    override fun initialize() {
        super.initialize()
        // Initialize FileKit
        FileKit.init(appId = "com.serratocreations.phovo")
        applicationScope.launch {
            val serverConfig = serverConfigRepository.value.observeServerConfig().first()
            // If this server has been configured restart it
            serverConfig?.let { availableServerConfig ->
                desktopServerConfigManager.value.configureDeviceAsServer(availableServerConfig)
            }
        }
        applicationScope.launch {
            // Suspends until a config is available(consider withTimeout)
            val backupDirectory = serverConfigRepository.value.observeServerConfig()
                .mapNotNull { it?.backupDirectory }
                .distinctUntilChanged()
                .first()
            localMediaManager.value.initMediaProcessing(
                outputDirectory = backupDirectory.absolutePath()
            )
        }
    }
}