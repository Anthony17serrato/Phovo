package com.serratocreations.phovo

import com.serratocreations.phovo.core.workmanager.IosPhovoWorkManager
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.server.ServerAddressResolver
import kotlinx.coroutines.CoroutineScope

class IosAppInitializer(
    private val workManager: Lazy<IosPhovoWorkManager>,
    applicationScope: CoroutineScope,
    serverAddressResolver: Lazy<ServerAddressResolver>,
    localAndRemoteMediaRepository: Lazy<LocalAndRemoteMediaRepository>,
    permissionRepository: Lazy<PermissionRepository>
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
        workManager.value.registerBackgroundTasks()
        super.initialize()
    }
}