package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.IosAndroidLocalMediaManager
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalAndRemoteMediaRepositoryImpl
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal expect fun getAndroidIosModules(): Module
internal actual fun getAndroidDesktopIosModules(): Module = module {
    includes(getAndroidIosModules())

    single {
        LocalAndRemoteMediaRepositoryImpl(
            localMediaRepository = get(),
            remoteMediaRepository = get(),
            applicationScope = get(APPLICATION_SCOPE),
            ioDispatcher = get(IO_DISPATCHER)
        )
    } binds arrayOf(
        LocalAndRemoteMediaRepository::class,
        MediaRepository::class
    )

    single {
        IosAndroidLocalMediaManager(
            localAndRemoteMediaRepository = get(),
            localMediaRepository = get(),
            localMediaProcessor = get(),
            appScope = get(APPLICATION_SCOPE),
            logger = get()
        )
    } binds arrayOf(
        LocalMediaManager::class,
        IosAndroidLocalMediaManager::class
    )
}