package com.serratocreations.phovo.core.domain.di

import com.serratocreations.phovo.core.domain.GetPhotosFeedWithThumbnailsUseCase
import com.serratocreations.phovo.core.domain.ServerGetPhotosFeedWithThumbnailsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    factory<GetPhotosFeedWithThumbnailsUseCase> {
        ServerGetPhotosFeedWithThumbnailsUseCase(
            mediaRepository = get(),
            serverConfigRepository = get(),
            logger = get()
        )
    }
}