package com.serratocreations.phovo

import com.serratocreations.phovo.core.workmanager.BackoffPolicy
import com.serratocreations.phovo.core.workmanager.Constraints
import com.serratocreations.phovo.core.workmanager.ExistingWorkPolicy
import com.serratocreations.phovo.core.workmanager.LongRunningInfo
import com.serratocreations.phovo.core.workmanager.NetworkType
import com.serratocreations.phovo.core.workmanager.OneTimeWorkRequest
import com.serratocreations.phovo.core.workmanager.PhovoWorkManager
import com.serratocreations.phovo.core.workmanager.PhovoWorker
import com.serratocreations.phovo.core.workmanager.WorkResult
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.server.ServerAddressResolver
import com.serratocreations.phovo.di.MEDIA_SYNC_WORKER_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class AndroidIosAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverAddressResolver: ServerAddressResolver,
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val permissionRepository: PermissionRepository,
    private val workManager: PhovoWorkManager
): AndroidDesktopIosAppInitializer() {
    override fun initialize() {
        super.initialize()
        serverAddressResolver.start()
        applicationScope.launch {
            localAndRemoteMediaRepository.clearNonFailedSyncLogs()
            // First await for permissions, there is no point in processing local media if permission
            // is missing
            permissionRepository.observeGalleryPermissionStatus()
                .map { status ->
                    status.permissionStatus == PermissionStatus.Granted || status.isLimited
                }
                .filter { it }
                .distinctUntilChanged()
                .collect {
                    workManager.enqueueUniqueWork(
                        uniqueWorkName = "media-sync-now",
                        policy = ExistingWorkPolicy.REPLACE,
                        request = OneTimeWorkRequest(
                            workerId = MEDIA_SYNC_WORKER_ID,
                            expedited = true,
                            longRunning = LongRunningInfo(
                                // TODO extract string resource
                                title = "Backing up photos",
                                subtitle = "Uploading to Phovo Desktop"
                            ),
                            constraints = Constraints(requiredNetworkType = NetworkType.UNMETERED),
                            backoffPolicy = BackoffPolicy.EXPONENTIAL,
                            backoffDelay = BackoffPolicy.MIN_BACKOFF_DELAY,
                            tags = setOf("media-sync-now")
                        )
                    )
                }
            // Code below this line is un-reachable
        }
    }
}

class MediaSyncWorker(
    private val localMediaManager: LocalMediaManager
) : PhovoWorker() {
    override suspend fun doWork(): WorkResult {
        coroutineScope {
            // TODO proper success fail implementation
            localMediaManager.apply {
                initMediaProcessing()
            }
        }
        return WorkResult.Success
    }
}