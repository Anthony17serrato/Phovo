package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.database.di.getDatabaseModule
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepositoryImpl
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * In addition to common definitions for IOS, Desktop, & Android
 * this API provides modules that are specific to each individual platform
 */
internal expect fun getAndroidDesktopIosModules(): Module

internal actual fun getPlatformModulesBranch1(): Module = module {
    includes(getAndroidDesktopIosModules(), getDatabaseModule())

    single {
        LocalMediaRepositoryImpl(
            localMediaDataSource = get(),
            logger = get()
        )
    } binds arrayOf(
        LocalMediaRepository::class,
        MediaRepository::class
    )
}