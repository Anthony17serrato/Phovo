package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.DEFAULT_DISPATCHER
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.network.IosAndroidMediaNetworkDataSource
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepositoryImpl
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepositoryImpl
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal expect fun getAndroidIosModules(): Module
internal actual fun getAndroidDesktopIosModules(): Module = module {
    includes(getAndroidIosModules())

    single {
        RemoteMediaRepositoryImpl(
            remotePhotosDataSource = get(),
            serverConfigRepository = get(),
            applicationScope = get(APPLICATION_SCOPE),
            logger = get()
        )
    } binds arrayOf(
        RemoteMediaRepositoryImpl::class,
        RemoteMediaRepository::class
    )

    single {
        LocalAndRemoteMediaRepositoryImpl(
            localMediaRepository = get(),
            remoteMediaRepository = get(),
            applicationScope = get(APPLICATION_SCOPE),
            ioDispatcher = get(IO_DISPATCHER),
            defaultDispatcher = get(DEFAULT_DISPATCHER),
            logger = get()
        )
    } binds arrayOf(
        LocalAndRemoteMediaRepository::class,
        MediaRepository::class
    )

    single {
        LocalMediaManager(
            localAndRemoteMediaRepository = get(),
            localMediaProcessor = get(),
            appScope = get(APPLICATION_SCOPE),
            logger = get()
        )
    } binds arrayOf(
        LocalMediaManager::class
    )

    single<MediaNetworkDataSource> {
        IosAndroidMediaNetworkDataSource(
            get(),
            get(),
            get(IO_DISPATCHER)
        )
    }
}