package com.serratocreations.phovo

import com.serratocreations.phovo.core.serverconfig.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope

abstract class AndroidDesktopIosAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverConfigRepository: ServerConfigRepository
): AndroidDesktopIosWasmAppInitializer() {

    override fun initialize() {
        super.initialize()
    }
}