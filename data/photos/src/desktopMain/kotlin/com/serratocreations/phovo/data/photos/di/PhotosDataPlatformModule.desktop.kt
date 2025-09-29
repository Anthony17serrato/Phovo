package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.LocalMediaManager
import com.serratocreations.phovo.data.photos.local.DesktopLocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.LocalSupportMediaRepository
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosModules(): Module = module {
    single {
        HttpClient(OkHttp)
    }

    single<MediaNetworkDataSource> {
        MediaNetworkDataSource(client = get(), logger = get())
    }

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

    single {
        LocalSupportMediaRepository(
            localMediaDataSource = get(),
            remoteMediaDataSource = get(),
            logger = get(),
            ioDispatcher = get(IO_DISPATCHER)
        )
    } binds arrayOf(
        LocalSupportMediaRepository::class,
        MediaRepository::class
    )
}