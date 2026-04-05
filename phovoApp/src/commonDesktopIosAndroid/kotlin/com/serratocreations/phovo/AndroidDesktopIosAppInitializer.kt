package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.server.data.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope

abstract class AndroidDesktopIosAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverConfigRepository: ServerConfigRepository,
    private val localMediaManager: LocalMediaManager
): AndroidDesktopIosWasmAppInitializer() {

    override fun initialize() {
        super.initialize()
    }
}