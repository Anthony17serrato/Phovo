package com.serratocreations.phovo.di

import com.serratocreations.phovo.MediaBackupWorker
import com.serratocreations.phovo.core.workmanager.WorkerRegistration
import com.serratocreations.phovo.core.workmanager.di.getWorkManagerModule
import com.serratocreations.phovo.data.permissions.di.getPermissionsDataModule
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val MEDIA_BACKUP_WORKER_ID = "media-backup"
abstract class IosAndroidApplicationPlatformModuleFetcher: ApplicationPlatformModuleFetcher() {
    override fun getModule(): Module = module {
        includes(super.getModule(), getPermissionsDataModule(), getWorkManagerModule())

        single(named(MEDIA_BACKUP_WORKER_ID)) {
            WorkerRegistration(MEDIA_BACKUP_WORKER_ID) {
                MediaBackupWorker(
                    localMediaManager = get(),
                    localAndRemoteMediaRepository = get()
                )
            }
        }
    }
}
