package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope

class IosAppInitializer(
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

    }
}