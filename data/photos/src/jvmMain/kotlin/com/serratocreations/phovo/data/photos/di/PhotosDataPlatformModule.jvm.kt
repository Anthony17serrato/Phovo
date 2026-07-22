package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.DesktopLocalMediaManager
import com.serratocreations.phovo.data.photos.local.DesktopLocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.util.DesktopFileHashCalculator
import com.serratocreations.phovo.data.photos.util.FileHashCalculator
import com.serratocreations.phovo.data.thumbnails.di.thumbnailsModule
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosModules(): Module = module {
//    Not used currently
//    single {
//        HttpClient(OkHttp)
//    }

    includes(thumbnailsModule)
    single<DesktopLocalMediaManager> {
        DesktopLocalMediaManager(
            localMediaRepository = get(),
            localMediaProcessor = get(),
            ioDispatcher = get(IO_DISPATCHER),
            fileHashCalculator = get(),
            appScope = get(APPLICATION_SCOPE),
            logger = get()
        )
    }

    single<MediaRepository> {
        get<LocalMediaRepository>()
    }

    factory<FileHashCalculator> {
        DesktopFileHashCalculator(ioDispatcher = get(IO_DISPATCHER))
    }

    single<DesktopLocalMediaProcessor> {
        val ioDispatcher: CoroutineDispatcher = get(IO_DISPATCHER)
        DesktopLocalMediaProcessor(
            thumbnailRepository = get(),
            ioDispatcher = ioDispatcher,
            fileHashCalculator = get(),
            logger = get()
        )
    }

    single<com.serratocreations.phovo.core.common.LocalMediaSyncTrigger> {
        object : com.serratocreations.phovo.core.common.LocalMediaSyncTrigger {
            override fun triggerSync() {}
        }
    }
}