package com.serratocreations.phovo

import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.server.ServerAddressResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AndroidAppInitializer(
    private val applicationScope: CoroutineScope,
    private val localMediaManager: LocalMediaManager,
    private val serverAddressResolver: ServerAddressResolver
): AndroidDesktopIosAppInitializer() {

    override fun initialize() {
        super.initialize()
        // Android specific initialization
        serverAddressResolver.start()
        applicationScope.launch {
            localMediaManager.initMediaProcessing()
        }
    }
}