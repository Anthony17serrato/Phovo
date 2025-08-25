package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.local.AndroidLocalPhotoProvider
import com.serratocreations.phovo.data.photos.local.LocalPhotoProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPhotosDataPlatformSubModule(): Module = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    single<LocalPhotoProvider> {
        AndroidLocalPhotoProvider(context = get())
    }
}