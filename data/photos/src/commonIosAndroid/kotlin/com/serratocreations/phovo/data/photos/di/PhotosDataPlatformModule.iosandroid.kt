package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.repository.CommonLocalSupportMediaRepository
import com.serratocreations.phovo.data.photos.repository.IosAndroidLocalSupportMediaRepository
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal expect fun getAndroidIosModules(): Module
internal actual fun getAndroidDesktopIosModules(): Module = module {
    includes(getAndroidIosModules())
    single {
        val appScope: CoroutineScope = get(APPLICATION_SCOPE)
        IosAndroidLocalSupportMediaRepository(
            localMediaProcessor = get(),
            localMediaDataSource = get(),
            remoteMediaDataSource = get(),
            logger = get(),
            appScope = appScope,
            ioDispatcher = get(IO_DISPATCHER)
        )
    } binds arrayOf(
        CommonLocalSupportMediaRepository::class,
        LocalSupportMediaRepository::class,
        MediaRepository::class
    )
}