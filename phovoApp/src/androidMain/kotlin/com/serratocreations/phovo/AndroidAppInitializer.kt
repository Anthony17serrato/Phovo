package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import kotlinx.coroutines.CoroutineScope
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository

class AndroidAppInitializer(
    applicationScope: CoroutineScope,
    serverConfigRepository: ServerConfigRepository,
    localMediaManager: LocalMediaManager
): AndroidDesktopIosAppInitializer(
    applicationScope,
    serverConfigRepository,
    localMediaManager
) {

    override fun initialize() {
        super.initialize()
        // Android specific initialization
    }
}