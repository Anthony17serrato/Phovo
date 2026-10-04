package com.serratocreations.phovo

import com.serratocreations.phovo.core.common.performance.ProcessingCpuBudget
import com.serratocreations.phovo.core.domain.GetBackupStatusUseCase
import com.serratocreations.phovo.core.domain.model.BackupStatus
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.core.model.network.isConnected
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

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
    private val getBackupStatusUseCase: GetBackupStatusUseCase,
    private val localMediaManager: LocalMediaManager,
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val cpuBudget: ProcessingCpuBudget,
) : PhovoWorker() {
    @OptIn(FlowPreview::class)
    override suspend fun doWork(): WorkResult {
        return coroutineScope {
            launch {
                // TODO media processing needs to post progress too otherwise IOS will kill
                //  the worker
                localAndRemoteMediaRepository.syncByteProgress
                    .sample(PROGRESS_REPORT_INTERVAL)
                    .collect {
                        setProgress(completed = it.completedBytes, total = it.totalBytes)
                    }
            }
            // todo this approach could lead to OOM ,implement a more memory efficient way to check if media
            //  is already processed(refer to desktop media processing implementation)
            val alreadyProcessedLocalItems = localAndRemoteMediaRepository.phovoMediaFlow().first()
            val processingJob = with(localMediaManager) {
                processJob(
                    localItems = alreadyProcessedLocalItems,
                )
            }
            val canSyncStart = withTimeoutOrNull(SERVER_CONNECTION_TIMEOUT) {
                localAndRemoteMediaRepository.observeConnectionState()
                    .takeWhile { it !is ServerConnectionState.NotConfigured }
                    .map { it.isConnected }
                    .firstOrNull { it }
            }
            val syncJob = if (canSyncStart == true) {
                with(localMediaManager) {
                    syncJob(processingJob)
                }
            } else null
            processingJob.join()

            var result: WorkResult = WorkResult.Success
            if (syncJob != null) {
                // Kill sync if server connection goes bad & processing job is done already
                getBackupStatusUseCase().takeWhile { status ->
                    when (status) {
                        is BackupStatus.BackupCompleteLocal, BackupStatus.Initializing,
                        is BackupStatus.LocalMediaBackupProgress, BackupStatus.Scanning -> true
                        BackupStatus.ServerOffline -> {
                            syncJob.cancel()
                            result = WorkResult.Retry
                            false
                        }
                    }
                }.collect()
            }
            coroutineContext.cancelChildren()
            return@coroutineScope result
        }
    }

    private companion object {
        /** About the cadence measured to keep an iOS background task alive on battery. */
        val PROGRESS_REPORT_INTERVAL = 1.seconds
        val SERVER_CONNECTION_TIMEOUT = 10.seconds
    }
}