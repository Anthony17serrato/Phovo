package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosWasmModules(): Module = module {
    single<MediaRepository> {
        MediaRepository(
            remotePhotosDataSource = get()
        )
    }

    single {
        HttpClient(Js) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    single {
        MediaNetworkDataSource(client = get(), logger = get())
    }
}