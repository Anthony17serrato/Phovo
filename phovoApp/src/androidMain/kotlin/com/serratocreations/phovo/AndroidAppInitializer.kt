package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import kotlinx.coroutines.CoroutineScope
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import kotlinx.coroutines.launch

class AndroidAppInitializer(
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
        // Android specific initialization
        applicationScope.launch {
            localMediaManager.initMediaProcessing(null)
        }
    }
}