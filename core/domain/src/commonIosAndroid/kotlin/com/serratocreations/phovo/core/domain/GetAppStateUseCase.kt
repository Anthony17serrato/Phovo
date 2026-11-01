package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.model.PhovoAppState
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine


/**
 * User visible Phovo status represents the user visible current state of the application, it includes
 * any ongoing operations such as processing and sync in addition to notifying issues with server or
 * Network connection.
 */
class GetAppStateUseCase(
    private val localAndRemoteMediaRepository: LocalAndRemoteMediaRepository,
    private val localMediaManager: LocalMediaManager
) {
    operator fun invoke(): Flow<PhovoAppState> {
        return combine(
            localAndRemoteMediaRepository.observeConnectionState(),
            localMediaManager.backupState
        ) { connectionState, backupState ->
            when (connectionState) {
                is ServerConnectionState.Connected -> {
                    val syncState = backupState.syncState
                    if (backupState.isProcessing && syncState is SyncState.Syncing && syncState.totalSyncJobQuantity == 0) {
                        PhovoAppState.SyncAwaitingItemsFromProcessingJob
                    } else {
                        when(val smartCastSyncState = backupState.syncState) {
                            is SyncState.SyncComplete -> {
                                PhovoAppState.BackupCompleteLocal(
                                    backedUpQuantity = smartCastSyncState.backedUpQuantity,
                                    failureQuantity = smartCastSyncState.failureQuantity
                                )
                            }
                            is SyncState.Syncing -> {
                                PhovoAppState.LocalMediaBackupProgress(
                                    syncedCount = smartCastSyncState.syncedCount,
                                    currentPendingSyncQuantity = smartCastSyncState.currentPendingSyncQuantity,
                                    scanningStatus = backupState.isProcessing
                                )
                            }
                            is SyncState.Waiting -> {
                                PhovoAppState.CheckingServerConnection(backupState.isProcessing)
                            }
                        }
                    }
                }
                ServerConnectionState.Checking -> PhovoAppState.CheckingServerConnection(scanningStatus = backupState.isProcessing)
                ServerConnectionState.NotConfigured -> PhovoAppState.Unconfigured(scanningStatus = backupState.isProcessing)
                // Unreachable and answered-by-the-wrong-server
                // are all "no backup happening" as far as the user is concerned; none of them imply
                // a different action here.
                is ServerConnectionState.Unreachable,
                ServerConnectionState.IdentityMismatch -> PhovoAppState.ServerOffline(scanningStatus = backupState.isProcessing)
            }
        }
    }
}

