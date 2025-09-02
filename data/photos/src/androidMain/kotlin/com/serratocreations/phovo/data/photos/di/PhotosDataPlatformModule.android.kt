package com.serratocreations.phovo.data.photos.di

import com.serratocreations.phovo.core.common.di.IO_DISPATCHER
import com.serratocreations.phovo.data.photos.local.AndroidLocalMediaProcessor
import com.serratocreations.phovo.data.photos.local.LocalMediaProcessor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidIosModules(): Module = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    single<LocalMediaProcessor> {
        AndroidLocalMediaProcessor(ioDispatcher = get(IO_DISPATCHER), context = get())
    }
}