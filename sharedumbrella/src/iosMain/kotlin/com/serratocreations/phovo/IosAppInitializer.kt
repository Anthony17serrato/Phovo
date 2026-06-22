package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class IosAppInitializer(
    private val applicationScope: CoroutineScope,
    serverConfigRepository: ServerConfigRepository,
    private val localMediaManager: LocalMediaManager
): AndroidDesktopIosAppInitializer() {
    override fun initialize() {
        super.initialize()
        applicationScope.launch {
            localMediaManager.initMediaProcessing()
        }
    }
}