package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import kotlinx.coroutines.CoroutineScope
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository

class AndroidAppInitializer(
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
        // Android specific initialization
    }
}