package com.serratocreations.phovo

import com.serratocreations.phovo.core.workmanager.IosPhovoWorkManager
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.server.ServerAddressResolver
import kotlinx.coroutines.CoroutineScope

class IosAppInitializer(
    private val workManager: IosPhovoWorkManager,
    applicationScope: CoroutineScope,
    serverAddressResolver: ServerAddressResolver,
    localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    permissionRepository: PermissionRepository
): AndroidIosAppInitializer(
    applicationScope,
    serverAddressResolver,
    localAndRemoteMediaRepository,
    permissionRepository,
    workManager
) {
    override fun initialize() {
        // BGTaskScheduler only accepts handler registration before the app finishes launching, and
        // this runs inside iOSApp.swift's init(), so it has to happen here rather than lazily.
        workManager.registerBackgroundTasks()
        super.initialize()
    }
}