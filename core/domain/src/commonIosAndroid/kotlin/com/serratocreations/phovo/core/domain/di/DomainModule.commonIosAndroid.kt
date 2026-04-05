package com.serratocreations.phovo.core.domain.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.core.domain.ClientGetPhotosFeedWithThumbnailsUseCase
import com.serratocreations.phovo.core.domain.GetBackupStatusUseCase
import com.serratocreations.phovo.core.domain.GetPhotosFeedWithThumbnailsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    factory<GetBackupStatusUseCase> {
        GetBackupStatusUseCase(
            iosAndroidLocalMediaManager = get(),
            remoteMediaRepository = get()
        )
    }

    factory<GetPhotosFeedWithThumbnailsUseCase> {
        ClientGetPhotosFeedWithThumbnailsUseCase(
            mediaRepository = get(),
            serverConfigRepository = get(),
            ioDispatcher = get(IO_DISPATCHER),
            logger = get()
        )
    }
}