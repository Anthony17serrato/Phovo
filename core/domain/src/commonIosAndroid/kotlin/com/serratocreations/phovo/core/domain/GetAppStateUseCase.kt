package com.serratocreations.phovo.core.domain

import com.serratocreations.phovo.core.domain.model.PhovoAppState
import com.serratocreations.phovo.core.model.network.ServerConnectionState
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.model.BackupState
import com.serratocreations.phovo.data.photos.model.ProcessingState
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
            // Checked before the connection: without gallery access nothing can be backed up, so
            // a server problem would point the user at the wrong fix.
            when (backupState) {
                BackupState.MediaAccessRequired -> PhovoAppState.MediaAccessRequired
                is BackupState.Active -> backupState.toAppState(connectionState)
            }
        }
    }

    private fun BackupState.Active.toAppState(
        connectionState: ServerConnectionState
    ): PhovoAppState {
        val smartCastProcessing = processing // Enables smart cast where needed
        return when (connectionState) {
            is ServerConnectionState.Connected -> {
                val syncState = syncState
                if (
                    smartCastProcessing is ProcessingState.Running &&
                    syncState is SyncState.Syncing &&
                    syncState.totalSyncJobQuantity == 0
                ) {
                    PhovoAppState.SyncAwaitingItemsFromProcessingJob(smartCastProcessing)
                } else {
                    when (syncState) {
                        is SyncState.SyncComplete -> {
                            PhovoAppState.BackupCompleteLocal(
                                backedUpQuantity = syncState.backedUpQuantity,
                                failureQuantity = syncState.failureQuantity
                            )
                        }
                        is SyncState.Syncing -> {
                            PhovoAppState.LocalMediaBackupProgress(
                                syncedCount = syncState.syncedCount,
                                currentPendingSyncQuantity = syncState.currentPendingSyncQuantity,
                                processing = smartCastProcessing
                            )
                        }
                        is SyncState.Waiting -> {
                            PhovoAppState.CheckingServerConnection(smartCastProcessing)
                        }
                    }
                }
            }
            ServerConnectionState.Checking -> PhovoAppState.CheckingServerConnection(smartCastProcessing)
            ServerConnectionState.NotConfigured -> PhovoAppState.Unconfigured(smartCastProcessing)
            // Unreachable and answered-by-the-wrong-server
            // are all "no backup happening" as far as the user is concerned; none of them imply
            // a different action here.
            is ServerConnectionState.Unreachable,
            ServerConnectionState.IdentityMismatch -> PhovoAppState.ServerOffline(smartCastProcessing)
        }
    }
}
