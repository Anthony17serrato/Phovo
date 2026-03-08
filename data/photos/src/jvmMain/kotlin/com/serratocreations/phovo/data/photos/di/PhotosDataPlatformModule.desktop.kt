package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.local.DesktopLocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.repository.LocalMediaRepository
import com.serratocreations.phovo.data.photos.repository.MediaRepository
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
    single<LocalMediaManager> {
        LocalMediaManager(
            get(),
            get(),
            get(APPLICATION_SCOPE),
            get()
        )
    }

    single<MediaRepository> {
        get<LocalMediaRepository>()
    }

    single<LocalMediaProcessor> {
        val ioDispatcher: CoroutineDispatcher = get(IO_DISPATCHER)
        DesktopLocalMediaProcessor(
            thumbnailRepository = get(),
            logger = get(),
            ioDispatcher = ioDispatcher
        )
    }
}

internal actual fun getPlatformModulesBranch2(): Module = module {

}