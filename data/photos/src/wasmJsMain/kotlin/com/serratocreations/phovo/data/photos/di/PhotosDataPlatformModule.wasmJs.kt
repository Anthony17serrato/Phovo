package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.repository.MediaRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPhotosDataPlatformModule(): Module = module {
    single<MediaRepository> {
        MediaRepository(
            remotePhotosDataSource = get(),
            appScope = get()
        )
    }

    single {
        HttpClient(Js) {
            install(ContentNegotiation) {
                json()
            }
        }
    }
}