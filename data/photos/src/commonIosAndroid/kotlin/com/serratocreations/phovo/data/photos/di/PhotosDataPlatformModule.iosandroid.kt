package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.IosAndroidLocalMediaManager
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.network.IosAndroidMediaNetworkDataSource
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.IosAndroidLocalSupportMediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal expect fun getAndroidIosModules(): Module
internal actual fun getAndroidDesktopIosModules(): Module = module {
    includes(getAndroidIosModules())

    single {
        IosAndroidMediaNetworkDataSource(client = get(), logger = get())
    } binds arrayOf(
        IosAndroidMediaNetworkDataSource::class,
        MediaNetworkDataSource::class
    )

    single {
        IosAndroidLocalSupportMediaRepository(
            localMediaDataSource = get(),
            remoteMediaDataSource = get(),
            logger = get(),
            ioDispatcher = get(IO_DISPATCHER)
        )
    } binds arrayOf(
        IosAndroidLocalSupportMediaRepository::class,
        LocalSupportMediaRepository::class,
        MediaRepository::class
    )

    single {
        IosAndroidLocalMediaManager(
            get(),
            get(),
            get(APPLICATION_SCOPE),
            get()
        )
    } binds arrayOf(
        LocalMediaManager::class,
        IosAndroidLocalMediaManager::class
    )
}