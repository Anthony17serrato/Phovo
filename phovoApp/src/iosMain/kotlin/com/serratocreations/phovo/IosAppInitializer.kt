package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class IosAppInitializer(
    private val applicationScope: CoroutineScope,
    serverConfigRepository: ServerConfigRepository,
    private val localMediaManager: LocalMediaManager
): AndroidDesktopIosAppInitializer(
    applicationScope,
    serverConfigRepository,
    localMediaManager
) {
    override fun initialize() {
        super.initialize()
        applicationScope.launch {
            localMediaManager.initMediaProcessing(null)
        }
    }
}