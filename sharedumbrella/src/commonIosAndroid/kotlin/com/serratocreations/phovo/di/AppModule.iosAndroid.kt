package com.serratocreations.phovo.di

import com.serratocreations.phovo.MediaSyncWorker
import com.serratocreations.phovo.core.workmanager.WorkerRegistration
import com.serratocreations.phovo.core.workmanager.di.getWorkManagerModule
import com.serratocreations.phovo.data.permissions.di.getPermissionsDataModule
import org.koin.core.module.Module
import org.koin.dsl.module

const val MEDIA_SYNC_WORKER_ID = "media-sync"
abstract class IosAndroidApplicationPlatformModuleFetcher: ApplicationPlatformModuleFetcher() {
    override fun getModule(): Module = module {
        includes(super.getModule(), getPermissionsDataModule(), getWorkManagerModule())

        single { WorkerRegistration(MEDIA_SYNC_WORKER_ID) { MediaSyncWorker(get()) } }
    }
}