package com.serratocreations.phovo.data.thumbnails.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.thumbnails.FfmpegThumbnailGenerator
import com.serratocreations.phovo.data.thumbnails.ThumbnailRepository
import org.koin.dsl.module

val thumbnailsModule = module {
    single<ThumbnailRepository> {
        ThumbnailRepository(
            thumbnailGenerator = get()
        )
    }
    single<FfmpegThumbnailGenerator> {
        FfmpegThumbnailGenerator(
            ioDispatcher = get(IO_DISPATCHER),
            appScope = get(APPLICATION_SCOPE)
        )
    }
}