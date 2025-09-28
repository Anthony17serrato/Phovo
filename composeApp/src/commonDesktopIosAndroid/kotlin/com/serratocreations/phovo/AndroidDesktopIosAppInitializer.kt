package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

abstract class AndroidDesktopIosAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverConfigRepository: ServerConfigRepository,
    private val localSupportMediaRepository: LocalSupportMediaRepository
): AndroidDesktopIosWasmAppInitializer() {

    override fun initialize() {
        super.initialize()
        // TODO Should probably be moved to a periodic work manager
        applicationScope.launch {
            // Suspends until a config is available(consider withTimeout)
            val backupDirectory = serverConfigRepository.observeServerConfig()
                .mapNotNull { it?.backupDirectory }
                .distinctUntilChanged()
                .firstOrNull()
            localSupportMediaRepository.initMediaProcessing(backupDirectory)
        }
    }
}