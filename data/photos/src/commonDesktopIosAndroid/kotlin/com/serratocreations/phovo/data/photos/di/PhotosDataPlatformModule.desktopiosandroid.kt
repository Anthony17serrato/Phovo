package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepositoryImpl
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * In addition to common definitions for IOS, Desktop, & Android
 * this API provides modules that are specific to each individual platform
 */
expect fun getPhotosDataPlatformSubModule(): Module

actual fun getPhotosDataPlatformModule(): Module = module {
    includes(getPhotosDataPlatformSubModule())

    single {
        val appScope: CoroutineScope = get(APPLICATION_SCOPE)
        LocalSupportMediaRepositoryImpl(
            localUnprocessedMediaProvider = get(),
            localProcessedMediaDataSource = get(),
            remotePhotosDataSource = get(),
            logger = get(),
            appScope = appScope
        )
    } binds arrayOf(LocalSupportMediaRepository::class, MediaRepository::class)
}