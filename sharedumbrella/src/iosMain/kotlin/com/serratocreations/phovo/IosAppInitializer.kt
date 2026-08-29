package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.server.ServerAddressResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class IosAppInitializer(
    private val applicationScope: CoroutineScope,
    private val localMediaManager: LocalMediaManager,
    private val serverAddressResolver: ServerAddressResolver
): AndroidDesktopIosAppInitializer() {
    override fun initialize() {
        super.initialize()
        serverAddressResolver.start()
        applicationScope.launch {
            localMediaManager.initMediaProcessing()
        }
    }
}