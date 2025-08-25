package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.data.photos.repository.LocalSupportPhovoItemRepository
import com.serratocreations.phovo.data.photos.repository.PhovoItemRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * In addition to common definitions for IOS, Desktop, & Android
 * provides modules that are specific to each individual platform
 */
expect fun getPhotosDataPlatformSubModule(): Module

actual fun getPhotosDataPlatformModule(): Module = module {
    includes(getPhotosDataPlatformSubModule())

    single<PhovoItemRepository> {
        val appScope: CoroutineScope = get(APPLICATION_SCOPE)
        LocalSupportPhovoItemRepository(
            localPhotosDataSource = get(),
            remotePhotosDataSource = get(),
            appScope = appScope
        )
    }
}