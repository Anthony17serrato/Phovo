package com.serratocreations.phovo

import com.serratocreations.phovo.core.workmanager.BackoffPolicy
import com.serratocreations.phovo.core.workmanager.Constraints
import com.serratocreations.phovo.core.workmanager.ExistingPeriodicWorkPolicy
import com.serratocreations.phovo.core.workmanager.ExistingWorkPolicy
import com.serratocreations.phovo.core.workmanager.LongRunningInfo
import com.serratocreations.phovo.core.workmanager.NetworkType
import com.serratocreations.phovo.core.workmanager.OneTimeWorkRequest
import com.serratocreations.phovo.core.workmanager.PeriodicWorkRequest
import com.serratocreations.phovo.core.workmanager.PhovoWorkManager
import com.serratocreations.phovo.core.workmanager.PhovoWorker
import com.serratocreations.phovo.core.workmanager.WorkResult
import com.serratocreations.phovo.data.permissions.PermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionStatus
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.model.BackupState
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.SyncState
import com.serratocreations.phovo.data.server.ServerAddressResolver
import com.serratocreations.phovo.di.MEDIA_BACKUP_WORKER_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private const val MEDIA_BACKUP_WORK = "media-backup"
private const val MEDIA_BACKUP_PERIODIC_WORK = "media-backup-periodic"

abstract class AndroidIosAppInitializer(
    private val applicationScope: CoroutineScope,
    private val serverAddressResolver: Lazy<ServerAddressResolver>,
    private val localAndRemoteMediaRepository: Lazy<LocalAndRemoteMediaRepository>,
    private val permissionRepository: Lazy<PermissionRepository>,
    private val localMediaManager: Lazy<LocalMediaManager>,
    private val workManager: Lazy<PhovoWorkManager>
): AndroidDesktopIosAppInitializer() {

    private val isBackupBusy: Flow<Boolean> by lazy {
        localMediaManager.value.backupState.map { !it.isIdle }.distinctUntilChanged()
    }

    override fun initialize() {
        super.initialize()
        serverAddressResolver.value.start()
        initiateBackupWork()
    }

    private fun initiateBackupWork() = applicationScope.launch {
        // No point scheduling periodic backups until there is something to back up. Busy
        // detection below needs no gate: LocalMediaManager does not process without gallery access.
        permissionRepository.value.observeGalleryPermissionStatus()
            .first { status -> status.permissionStatus == PermissionStatus.Granted || status.isLimited }

        workManager.value.enqueueUniquePeriodicWork(
            uniqueWorkName = MEDIA_BACKUP_PERIODIC_WORK,
            policy = ExistingPeriodicWorkPolicy.KEEP,
            request = PeriodicWorkRequest(
                workerId = MEDIA_BACKUP_WORKER_ID,
                repeatInterval = 1.hours,
                initialDelay = 1.hours,
                constraints = Constraints(requiredNetworkType = NetworkType.UNMETERED),
                tags = setOf(MEDIA_BACKUP_PERIODIC_WORK)
            )
        )

        // If backup is busy we should bind that backup work to a work manager so that backgrounding
        // the application continues the backup.
        isBackupBusy.filter { it }
            .collect {
                workManager.value.enqueueUniqueWork(
                    uniqueWorkName = MEDIA_BACKUP_WORK,
                    policy = ExistingWorkPolicy.KEEP,
                    request = OneTimeWorkRequest(
                        workerId = MEDIA_BACKUP_WORKER_ID,
                        expedited = true,
                        longRunning = LongRunningInfo(
                            // TODO extract string resource
                            title = "Backing up photos",
                            subtitle = "Uploading to Phovo Desktop"
                        ),
                        backoffPolicy = BackoffPolicy.EXPONENTIAL,
                        backoffDelay = BackoffPolicy.MIN_BACKOFF_DELAY,
                        tags = setOf(MEDIA_BACKUP_WORK)
                    )
                )
            }
    }
}

class MediaBackupWorker(
    private val localMediaManager: LocalMediaManager,
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
) : PhovoWorker() {
    @OptIn(FlowPreview::class)
    override suspend fun doWork(): WorkResult = coroutineScope {
        launch {
            // TODO media processing needs to post progress too otherwise IOS will kill
            //  the worker
            localMediaManager.backupState
                .filterIsInstance<BackupState.Active>()
                .map { it.syncState }
                .filterIsInstance<SyncState.Syncing>()
                // TODO Temporary hardcode, address syncByteProgress TODO
                .map { it.syncByteProgress(true) }
                .sample(PROGRESS_REPORT_INTERVAL)
                .collect {
                    setProgress(completed = it.completedBytes, total = it.totalBytes)
                }
        }
        localMediaManager.joinOrStartBackup()
        coroutineContext.cancelChildren()
        WorkResult.Success
    }

    override suspend fun onLongRunningDenied(): WorkResult = WorkResult.Success

    private companion object {
        /** About the cadence measured to keep an iOS background task alive on battery. */
        val PROGRESS_REPORT_INTERVAL = 1.seconds
    }
}
