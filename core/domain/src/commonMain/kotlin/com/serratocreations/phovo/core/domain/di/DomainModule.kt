package com.serratocreations.phovo.core.domain.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.core.domain.GetPhotosFeedWithThumbnailsUseCase
import com.serratocreations.phovo.data.photos.di.getPhotosDataModule
import com.serratocreations.phovo.data.server.di.getServerDataModule
import org.koin.core.module.Module
import org.koin.dsl.module

val domainModule: Module = module {
    includes(getPhotosDataModule(), getServerDataModule(), platformModule)
    factory {
        GetPhotosFeedWithThumbnailsUseCase(
            mediaRepository = get(),
            serverConfigRepository = get(),
            ioDispatcher = get(IO_DISPATCHER),
            logger = get()
        )
    }
}

internal expect val platformModule: Module