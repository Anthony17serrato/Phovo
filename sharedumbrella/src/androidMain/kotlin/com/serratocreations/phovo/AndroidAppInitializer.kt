package com.serratocreations.phovo

import com.serratocreations.phovo.core.serverconfig.IosAndroidWasmServerConfigRepository
import com.serratocreations.phovo.data.photos.LocalMediaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AndroidAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverConfigRepository: IosAndroidWasmServerConfigRepository,
    private val localMediaManager: LocalMediaManager
): AndroidDesktopIosAppInitializer(
    applicationScope,
    serverConfigRepository
) {

    override fun initialize() {
        super.initialize()
        // Android specific initialization
        applicationScope.launch {
            localMediaManager.initMediaProcessing(
                localDirectory = null
            )
        }
    }
}