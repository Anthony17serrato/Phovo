package com.serratocreations.phovo.data.photos.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.serratocreations.phovo.data.photos")
internal actual class PhotosDataPlatformModule {
    @Singleton
    fun httpClient() = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json()
        }
    }
}