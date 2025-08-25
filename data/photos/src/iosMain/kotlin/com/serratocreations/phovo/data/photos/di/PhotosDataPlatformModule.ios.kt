package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.local.LocalPhotoProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import com.serratocreations.phovo.data.photos.local.IosLocalPhotoProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPhotosDataPlatformSubModule(): Module = module {
    single {
        HttpClient(Darwin) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    single<LocalPhotoProvider> {
        IosLocalPhotoProvider(
            logger = get(),
            ioDispatcher = get(IO_DISPATCHER)
        )
    }
}