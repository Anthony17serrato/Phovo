package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.local.DesktopLocalUnprocessedMediaProvider
import com.serratocreations.phovo.data.photos.local.LocalUnprocessedMediaProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPhotosDataPlatformSubModule(): Module = module {
    single {
        HttpClient(OkHttp)
    }

    single<LocalUnprocessedMediaProvider> {
        val ioDispatcher: CoroutineDispatcher = get(IO_DISPATCHER)
        DesktopLocalUnprocessedMediaProvider(logger = get(), ioDispatcher = ioDispatcher)
    }
}