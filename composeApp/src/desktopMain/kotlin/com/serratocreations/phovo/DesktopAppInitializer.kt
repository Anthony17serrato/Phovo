package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope

class DesktopAppInitializer(
    applicationScope: CoroutineScope,
    serverConfigRepository: ServerConfigRepository,
    localSupportMediaRepository: LocalSupportMediaRepository
): AndroidDesktopIosAppInitializer(
    applicationScope,
    serverConfigRepository,
    localSupportMediaRepository
) {
    override fun initialize() {
        super.initialize()

    }
}