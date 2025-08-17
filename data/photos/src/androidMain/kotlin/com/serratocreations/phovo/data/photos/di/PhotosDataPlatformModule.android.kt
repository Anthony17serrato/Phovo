package com.serratocreations.phovo.data.photos.di

import android.content.Context
import com.serratocreations.phovo.data.photos.local.AndroidLocalPhotoProvider
import com.serratocreations.phovo.data.photos.local.LocalPhotoProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.serratocreations.phovo.data.photos")
internal actual class PhotosDataPlatformModule {
    @Singleton
    fun httpClient() = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
    }

    // Workaround for Koin Annotations bug... Can't annotate class with @Singleton
    // https://github.com/InsertKoinIO/koin-annotations/issues/249
    @Singleton
    fun phovoItemDao(context: Context): LocalPhotoProvider = AndroidLocalPhotoProvider(context)
}