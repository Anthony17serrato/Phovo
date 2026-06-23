package com.serratocreations.phovo

import com.serratocreations.phovo.core.serverconfig.IosAndroidServerConfigRepository
import com.serratocreations.phovo.data.photos.LocalMediaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AndroidAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverConfigRepository: IosAndroidServerConfigRepository,
    private val localMediaManager: LocalMediaManager
): AndroidDesktopIosAppInitializer() {

    override fun initialize() {
        super.initialize()
        // Android specific initialization
        applicationScope.launch {
            localMediaManager.initMediaProcessing()
        }
    }
}