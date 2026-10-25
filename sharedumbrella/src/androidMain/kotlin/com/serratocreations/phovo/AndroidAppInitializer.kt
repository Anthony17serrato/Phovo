package com.serratocreations.phovo

import com.serratocreations.phovo.core.workmanager.PhovoWorkManager
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.server.ServerAddressResolver
import kotlinx.coroutines.CoroutineScope

class AndroidAppInitializer(
    applicationScope: CoroutineScope,
    serverAddressResolver: Lazy<ServerAddressResolver>,
    localAndRemoteMediaRepository: Lazy<LocalAndRemoteMediaRepository>,
    permissionRepository: Lazy<PermissionRepository>,
    localMediaManager: Lazy<LocalMediaManager>,
    workManager: Lazy<PhovoWorkManager>
): AndroidIosAppInitializer(
    applicationScope,
    serverAddressResolver,
    localAndRemoteMediaRepository,
    permissionRepository,
    localMediaManager,
    workManager
) {

    override fun initialize() {
        super.initialize()
        // Android specific initialization

    }
}