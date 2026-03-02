package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.data.photos.network.MediaNetworkDataSource
import com.serratocreations.phovo.data.photos.network.WebMediaNetworkDataSource
import com.serratocreations.phovo.data.photos.repository.MediaRepository
import com.serratocreations.phovo.data.photos.repository.RemoteMediaRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getPlatformModulesBranch1(): Module = module {

    single {
        HttpClient(Js) {
            expectSuccess = false
            install(ContentNegotiation) {
                json()
            }
        }
    }

    single<MediaRepository> {
        get<RemoteMediaRepository>()
    }

    single<MediaNetworkDataSource> {
        WebMediaNetworkDataSource(
            get(),
            get()
        )
    }
}