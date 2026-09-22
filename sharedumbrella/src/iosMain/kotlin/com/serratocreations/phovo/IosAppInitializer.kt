package com.serratocreations.phovo

import com.serratocreations.phovo.core.workmanager.IosPhovoWorkManager
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.server.ServerAddressResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class IosAppInitializer(
    private val applicationScope: CoroutineScope,
    private val localMediaManager: LocalMediaManager,
    private val serverAddressResolver: ServerAddressResolver,
    private val workManager: IosPhovoWorkManager
): AndroidDesktopIosAppInitializer() {
    override fun initialize() {
        super.initialize()
        // BGTaskScheduler only accepts handler registration before the app finishes launching, and
        // this runs inside iOSApp.swift's init(), so it has to happen here rather than lazily.
        workManager.registerBackgroundTasks()
        serverAddressResolver.start()
        applicationScope.launch {
            localMediaManager.initMediaProcessing()
        }
    }
}