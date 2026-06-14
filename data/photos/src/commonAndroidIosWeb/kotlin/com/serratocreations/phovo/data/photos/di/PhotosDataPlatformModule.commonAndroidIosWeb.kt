package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepositoryImpl
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getPlatformModulesBranch2(): Module = module {
    single {
        RemoteMediaRepositoryImpl(
            remotePhotosDataSource = get(),
            serverConfigRepository = get(),
            applicationScope = get(APPLICATION_SCOPE),
            logger = get()
        )
    } binds arrayOf(
        RemoteMediaRepository::class
    )
}