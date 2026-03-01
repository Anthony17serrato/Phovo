package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.local.DesktopLocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosModules(): Module = module {
//    Not used currently
//    single {
//        HttpClient(OkHttp)
//    }

    single<LocalMediaManager> {
        LocalMediaManager(
            get(),
            get(),
            get(APPLICATION_SCOPE),
            get()
        )
    }

    single<LocalMediaProcessor> {
        val ioDispatcher: CoroutineDispatcher = get(IO_DISPATCHER)
        DesktopLocalMediaProcessor(logger = get(), ioDispatcher = ioDispatcher)
    }
}

internal actual fun getPlatformModulesBranch2(): Module = module {

}