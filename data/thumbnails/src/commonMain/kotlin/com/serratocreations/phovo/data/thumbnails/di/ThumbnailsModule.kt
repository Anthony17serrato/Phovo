package com.serratocreations.phovo.data.thumbnails.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.thumbnails.BundledFfmpegTools
import com.serratocreations.phovo.data.thumbnails.FfmpegThumbnailGenerator
import com.serratocreations.phovo.data.thumbnails.FfprobeMediaProbe
import com.serratocreations.phovo.data.thumbnails.ThumbnailRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import org.koin.dsl.module

val thumbnailsModule = module {
    single<ThumbnailRepository> {
        ThumbnailRepository(
            thumbnailGenerator = get()
        )
    }
    single<BundledFfmpegTools> {
        BundledFfmpegTools(
            appScope = get(APPLICATION_SCOPE),
            ioDispatcher = get(IO_DISPATCHER),
            installDirectory = (FileKit.filesDir / "ffmpeg").file
        )
    }
    single<FfmpegThumbnailGenerator> {
        FfmpegThumbnailGenerator(
            tools = get(),
            ioDispatcher = get(IO_DISPATCHER),
            cpuBudget = get(),
            logger = get()
        )
    }
    single<FfprobeMediaProbe> {
        FfprobeMediaProbe(
            tools = get(),
            ioDispatcher = get(IO_DISPATCHER),
            logger = get()
        )
    }
}